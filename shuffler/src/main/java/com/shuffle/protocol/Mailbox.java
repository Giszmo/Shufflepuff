package com.shuffle.protocol;

import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.blame.BlameException;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 *
 *
 * Created by Daniel Krawisz on 1/22/16.
 */
public class Mailbox {
    final private Network network;
    final private SessionIdentifier session;
    final private SigningKey sk;
    final private Collection<VerificationKey> players; // The keys representing all the players.

    final private Queue<SignedPacket> delivered = new LinkedList<>(); // A queue of messages that has been delivered that we aren't ready to look at yet.
    final private Queue<SignedPacket> history = new LinkedList<>(); // All messages received (does not include those in delivered).
    final private Queue<SignedPacket> sent = new LinkedList<>();
    private boolean blameReceived = false;

    public Mailbox(SessionIdentifier session, SigningKey sk, Collection<VerificationKey> players, Network network) {
        this.sk = sk;
        this.session = session;
        this.network = network;
        this.players = players;
    }

    public boolean blameReceived() {
        return blameReceived;
    }

    public void broadcast(Message message, Phase phase) throws TimeoutError, CryptographyError, InvalidImplementationError {
        for (VerificationKey to : players) {
            // Don't send a message to myself!
            if (!to.equals(sk.VerificationKey())) {
                send(new Packet(message, session, phase, sk.VerificationKey(), to));
            }
        }
    }

    // Send a message into the network.
    public void send(Packet packet) throws TimeoutError, CryptographyError, InvalidImplementationError {
        SignedPacket signed = new SignedPacket(packet, sk.makeSignature(packet));

        // Don't send anything to ourselves or to a nonexistent player.
        if (!packet.recipient.equals(sk.VerificationKey()) && players.contains(packet.recipient)) {
            network.sendTo(packet.recipient, signed);
            sent.add(signed);
        }
    }

    // Get the next message from the phase we're in. It's possible for other players to get
    // ahead under some circumstances, so we have to keep their messages to look at later.
    // It always returns a blame packet if encountered.
    SignedPacket receiveNextPacket(Phase expectedPhase)
            throws FormatException, CryptographyError,
            InterruptedException, TimeoutError, InvalidImplementationError, ValueException, SignatureException {

        SignedPacket found = null;

        // Go through the queue of received messages if any are there.
        if (delivered.size() > 0) {
            Iterator<SignedPacket> i = delivered.iterator();
            while (i.hasNext()) {
                SignedPacket next = i.next();
                Packet packet = next.payload;

                // Return any that matches what we're looking for.
                if (expectedPhase == packet.phase) {
                    i.remove();
                    found = next;
                    break;
                }
            }
        }

        // Now we wait for the right message from the network, since we haven't already received it.
        if (found == null) {
            while (true) {
                SignedPacket next = network.receive();
                Packet packet = next.payload;
                Phase phase = packet.phase;

                if (!next.verify()) {
                    throw new SignatureException(next);
                }

                // Check that this is someone in the same session of this protocol as us.
                if (!session.equals(packet.session)) {
                    throw new ValueException(ValueException.Values.session, session.toString(), packet.session.toString());
                }

                // Check that this message is intended for us.
                if (!packet.recipient.equals(sk.VerificationKey())) {
                    throw new ValueException(ValueException.Values.recipient, sk.VerificationKey().toString(), packet.recipient.toString());
                }

                if (expectedPhase == phase || phase == Phase.Blame) {
                    found = next;
                    break;
                }

                delivered.add(next);
            }
        }

        history.add(found);
        if (found.payload.phase == Phase.Blame) {
            blameReceived = true;
        }
        return found;
    }

    // Get all packets history or received by phase. Used during blame phase.
    public List<SignedPacket> getPacketsByPhase(Phase phase) {
        List<SignedPacket> selection = new LinkedList<>();

        for (SignedPacket packet : history) {
            if (packet.payload.phase == phase) {
                selection.add(packet);
            }
        }

        for (SignedPacket packet : delivered) {
            if (packet.payload.phase == phase) {
                selection.add(packet);
            }
        }

        return selection;
    }

    // Wait to receive a message from a given player.
    public Message receiveFrom(VerificationKey from, Phase expectedPhase)
            throws TimeoutError,
            CryptographyError,
            FormatException,
            ValueException,
            InvalidImplementationError,
            InterruptedException,
            BlameException,
            SignatureException {

        Packet packet = receiveNextPacket(expectedPhase).payload;

        if (packet.phase == Phase.Blame && expectedPhase != Phase.Blame) {
            throw new BlameException(packet.signer, packet);
        }

        // If we receive a message, but it is not from the expected source, it might be a blame message.
        if (!from.equals(packet.signer)) {
            throw new ValueException(ValueException.Values.phase, packet.phase.toString(), expectedPhase.toString());
        }

        return packet.message;
    }

    // Receive messages from a set of players, which may come in any order.
    public Map<VerificationKey, Message> receiveFromMultiple(
            Set<VerificationKey> from,
            Phase expectedPhase,
            boolean blameInterrupt // Whether to stop if a blame message is received.
    )
            throws TimeoutError, CryptographyError, FormatException,
            InvalidImplementationError, ValueException, InterruptedException,
            ProtocolException, BlameException, SignatureException {

        // Collect the messages in here.
        Map<VerificationKey, Message> broadcasts = new HashMap<>();

        // Don't receive a message from myself.
        from.remove(sk.VerificationKey());

        while (from.size() > 0) {
            Packet packet = receiveNextPacket(expectedPhase).payload;
            if (expectedPhase != Phase.Blame && packet.phase == Phase.Blame) {
                if (blameInterrupt) {
                    throw new BlameException(packet.signer, packet);
                }
                continue;
            }
            VerificationKey sender = packet.signer;

            if(broadcasts.containsKey(sender)) {
                throw new ProtocolException();
            }
            broadcasts.put(sender, packet.message);
            from.remove(sender);
        }

        return broadcasts;
    }

    // When the blame phase it reached, there may be a lot of blame going around. This function
    // waits to receive all blame messages until a timeout exception is caught, and then returns
    // the list of blame messages, organized by player.
    public Map<VerificationKey, List<SignedPacket>> receiveAllBlame() throws
            InterruptedException,
            FormatException,
            ValueException,
            SignatureException {
        Map<VerificationKey, List<SignedPacket>> blame = new HashMap<>();
        for (VerificationKey player : players) {
            blame.put(player, new LinkedList<SignedPacket>());
        }

        // First get the blame messages in history.
        for (SignedPacket packet : history) {
            if (packet.payload.phase == Phase.Blame) {
                blame.get(packet.payload.signer).add(packet);
            }
        }

        // Then receive any more blame messages until there are no more.
        while(true) {
            try {
                SignedPacket next = receiveNextPacket(Phase.Blame);
                blame.get(next.payload.signer).add(next);
            } catch (TimeoutError e) {
                break;
            }
        }

        return blame;
    }
}
