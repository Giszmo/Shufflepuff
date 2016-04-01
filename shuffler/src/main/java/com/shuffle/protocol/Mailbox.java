/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.blame.BlameException;
import com.shuffle.protocol.blame.Reason;

import java.net.ProtocolException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * A class for managing messages for the core protocol.
 *
 * Created by Daniel Krawisz on 1/22/16.
 */
public class Mailbox {
    private final Network network;
    private final SessionIdentifier session;
    private final SigningKey sk;
    private final Collection<VerificationKey> players; // The keys representing all the players.

    // A queue of messages that has been delivered that we aren't ready to look at yet.
    private final Queue<SignedPacket> delivered = new LinkedList<>();

    // All messages received (does not include those in delivered).
    private final Queue<SignedPacket> history = new LinkedList<>();

    private final Set<Reason> blame = new HashSet<>();

    public Mailbox(
            SessionIdentifier session,
            SigningKey sk,
            Collection<VerificationKey> players,
            Network network) {

        this.sk = sk;
        this.session = session;
        this.network = network;
        this.players = players;
    }

    // Whether a blame message with the given reason has been received.
    public boolean blame(Reason reason) {
        return blame.contains(reason);
    }

    public boolean blame() {
        return blame.size() > 0;
    }

    public void broadcast(Message message, Phase phase)
            throws TimeoutException, CryptographyError, InvalidImplementationError, InterruptedException {

        for (VerificationKey to : players) {
            send(new Packet(message, session, phase, sk.VerificationKey(), to));
        }
    }

    // Send a message into the network.
    public void send(Packet packet)
            throws TimeoutException, CryptographyError, InvalidImplementationError, InterruptedException {

        SignedPacket signed = new SignedPacket(packet, sk.makeSignature(packet));

        // Don't send anything to a nonexistent player.
        if (!players.contains(packet.recipient)) {
            return;
        }

        // If this is a message to myself, don't send it. Just pretend we received it.
        // This is useful later when we have to collect all blame messages later.
        if (packet.recipient.equals(sk.VerificationKey())) {
            history.add(signed);
            if (packet.phase == Phase.Blame) {
                try {
                    blame.add(packet.message.readBlame().reason);
                } catch (FormatException e) {
                    e.printStackTrace();
                }
            }
        } else {
            network.sendTo(packet.recipient, signed);
        }
    }

    // Get the next message from the phase we're in. It's possible for other players to get
    // ahead under some circumstances, so we have to keep their messages to look at later.
    // It always returns a blame packet if encountered.
    private SignedPacket receiveNextPacket(Phase expectedPhase)
            throws FormatException, CryptographyError,
            InterruptedException, InvalidImplementationError,
            ValueException, SignatureException {

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
                if (next == null) {
                    return null;
                }

                Packet packet = next.payload;
                Phase phase = packet.phase;

                if (!next.verify()) {
                    throw new SignatureException(next);
                }

                // Check that this is someone in the same session of this protocol as us.
                if (!session.equals(packet.session)) {
                    throw new ValueException(
                            ValueException.Values.session,
                            session.toString(), packet.session.toString());
                }

                // Check that this message is intended for us.
                if (!packet.recipient.equals(sk.VerificationKey())) {
                    throw new ValueException(
                            ValueException.Values.recipient,
                            sk.VerificationKey().toString(), packet.recipient.toString());
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

            try {
                blame.add(found.payload.message.readBlame().reason);
            } catch (FormatException e) {
                e.printStackTrace();
            }
        }
        return found;
    }

    // Get all packets history or received by phase. Used during blame phase.
    public Queue<SignedPacket> getPacketsByPhase(Phase phase) {
        Queue<SignedPacket> selection = new LinkedList<>();

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
            throws TimeoutException,
            CryptographyError,
            FormatException,
            ValueException,
            InvalidImplementationError,
            InterruptedException,
            BlameException,
            SignatureException {

        SignedPacket signed = receiveNextPacket(expectedPhase);
        if (signed == null) {
            throw new TimeoutException(from);
        }

        Packet packet = signed.payload;

        if (packet.phase == Phase.Blame && expectedPhase != Phase.Blame) {
            throw new BlameException(packet.signer, packet);
        }

        // Check signature.
        if (!from.equals(packet.signer)) {
            throw new ValueException(
                    ValueException.Values.phase,
                    packet.phase.toString(), expectedPhase.toString());
        }

        return packet.message;
    }

    // Wait to receive a message from a given player.
    public Message receiveFromBlameless(VerificationKey from, Phase expectedPhase)
            throws TimeoutException,
            CryptographyError,
            FormatException,
            ValueException,
            InvalidImplementationError,
            InterruptedException,
            SignatureException {

        Packet packet = null;
        do {
            SignedPacket signed = receiveNextPacket(expectedPhase);
            if (signed == null) throw new TimeoutException(from);

            packet = signed.payload;

        } while (expectedPhase != Phase.Blame && packet.phase == Phase.Blame);

        // Check signature.
        if (!from.equals(packet.signer)) {
            throw new ValueException(
                    ValueException.Values.phase,
                    packet.phase.toString(), expectedPhase.toString());
        }

        return packet.message;
    }

    // Receive messages from a set of players, which may come in any order.
    private Map<VerificationKey, Message> receiveFromMultiple(
            Set<VerificationKey> from,
            Phase expectedPhase,
            boolean ignoreBlame // Whether to stop if a blame message is received.
    )
            throws TimeoutException, CryptographyError, FormatException,
            InvalidImplementationError, ValueException, InterruptedException,
            ProtocolException, BlameException, SignatureException {

        // Collect the messages in here.
        Map<VerificationKey, SignedPacket> broadcasts = new HashMap<>();

        // Don't receive a message from myself.
        from.remove(sk.VerificationKey());

        while (from.size() > 0) {
            SignedPacket packet = receiveNextPacket(expectedPhase);
            if (packet == null) throw new TimeoutException(from);

            if (expectedPhase != Phase.Blame && packet.payload.phase == Phase.Blame) {
                if (!ignoreBlame) {
                    // Put the messages already collected back so that they can be received later.
                    for (SignedPacket p : broadcasts.values()) {
                        delivered.add(p);
                    }

                    throw new BlameException(packet.payload.signer, packet.payload);
                }
                continue;
            }
            VerificationKey sender = packet.payload.signer;

            if (broadcasts.containsKey(sender)) {
                throw new ProtocolException();
            }
            broadcasts.put(sender, packet);
            from.remove(sender);
        }

        // Strip the messages of signatures and routing information.
        Map<VerificationKey, Message> messages = new HashMap<>();

        for (Map.Entry<VerificationKey, SignedPacket> packet : broadcasts.entrySet()) {
            messages.put(packet.getKey(), packet.getValue().payload.message);
        }

        return messages;
    }

    public Map<VerificationKey, Message> receiveFromMultiple(
            Set<VerificationKey> from,
            Phase expectedPhase
    )
            throws TimeoutException, CryptographyError, FormatException,
            InvalidImplementationError, ValueException, InterruptedException,
            ProtocolException, BlameException, SignatureException {

        return receiveFromMultiple(from, expectedPhase, false);
    }

    public Map<VerificationKey, Message> receiveFromMultipleBlameless(
            Set<VerificationKey> from,
            Phase expectedPhase
    )
            throws TimeoutException, CryptographyError, FormatException,
            InvalidImplementationError, ValueException, InterruptedException,
            ProtocolException, SignatureException {

        try {
            return receiveFromMultiple(from, expectedPhase, true);
        } catch (BlameException ignored) {
        }

        assert true; // Should not reach here.
        return null;
    }

    // When the blame phase it reached, there may be a lot of blame going around. This function
    // waits to receive all blame messages until a timeout exception is caught, and then returns
    // the list of blame messages, organized by player, including those sent by the current player.
    public Map<VerificationKey, Queue<SignedPacket>> receiveAllBlame() throws
            InterruptedException,
            FormatException,
            ValueException,
            SignatureException {
        Map<VerificationKey, Queue<SignedPacket>> blame = new HashMap<>();
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
        while (true) {
            SignedPacket next = receiveNextPacket(Phase.Blame);
            if (next == null) break;

            blame.get(next.payload.signer).add(next);
        }

        return blame;
    }
}
