/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.blame.BlameException;
import com.shuffle.protocol.blame.Reason;
import com.shuffle.protocol.message.Message;
import com.shuffle.protocol.message.Packet;
import com.shuffle.protocol.message.Phase;

import java.io.IOException;
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
    private final Queue<Packet> delivered = new LinkedList<>();

    // All messages received (does not include those in delivered).
    private final Queue<Packet> history = new LinkedList<>();

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

    // Send a message into the network.
    public void send(Packet packet) throws IOException, InterruptedException {

        // Don't send anything to a nonexistent player.
        if (!players.contains(packet.recipient)) {
            return;
        }

        // If this is a message to myself, don't send it. Just pretend we received it.
        // This is useful later when we have to collect all blame messages later.
        if (packet.recipient.equals(sk.VerificationKey())) {
            history.add(packet);
            if (packet.phase == Phase.Blame) {
                try {
                    blame.add(packet.message.readBlame().reason);
                } catch (FormatException e) {
                    e.printStackTrace();
                }
            }
        } else {
            network.sendTo(packet.recipient, packet);
        }
    }

    public void broadcast(Message message, Phase phase) throws IOException, InterruptedException {

        for (VerificationKey to : players) {
            send(new Packet(message, session, phase, sk.VerificationKey(), to));
        }
    }

    // Get the next message from the phase we're in. It's possible for other players to get
    // ahead under some circumstances, so we have to keep their messages to look at later.
    // It always returns a blame packet if encountered.
    private Packet receiveNextPacket(Phase expectedPhase) throws InterruptedException, IOException {

        Packet found = null;

        // Go through the queue of received messages if any are there.
        if (delivered.size() > 0) {
            Iterator<Packet> i = delivered.iterator();
            while (i.hasNext()) {
                Packet packet = i.next();

                // Return any that matches what we're looking for.
                if (expectedPhase == packet.phase) {
                    i.remove();
                    found = packet;
                    break;
                }
            }
        }

        // Now we wait for the right message from the network, since we haven't already received it.
        if (found == null) {
            while (true) {
                Packet packet = network.receive();
                if (packet == null) {
                    return null;
                }

                Phase phase = packet.phase;

                // TODO: handle these next two checks at a lower level.
                // Check that this is someone in the same session of this protocol as us.
                if (!session.equals(packet.session)) {
                    continue;
                }

                // Check that this message is intended for us.
                if (!packet.recipient.equals(sk.VerificationKey())) {
                    continue;
                }

                if (expectedPhase == phase || phase == Phase.Blame) {
                    found = packet;
                    break;
                }

                delivered.add(packet);
            }
        }

        history.add(found);
        if (found.phase == Phase.Blame) {

            try {
                blame.add(found.message.readBlame().reason);
            } catch (FormatException e) {
                e.printStackTrace();
            }
        }
        return found;
    }

    // Get all packets history or received by phase. Used during blame phase.
    public Queue<Packet> getPacketsByPhase(Phase phase) {
        Queue<Packet> selection = new LinkedList<>();

        for (Packet packet : history) {
            if (packet.phase == phase) {
                selection.add(packet);
            }
        }

        for (Packet packet : delivered) {
            if (packet.phase == phase) {
                selection.add(packet);
            }
        }

        return selection;
    }

    // Wait to receive a message from a given player.
    public Message receiveFrom(VerificationKey from, Phase expectedPhase)
            throws BlameException, InterruptedException, IOException, WaitingException {

        Packet packet = receiveNextPacket(expectedPhase);

        if (packet == null) {
            throw new WaitingException(from);
        }

        if (packet.phase == Phase.Blame && expectedPhase != Phase.Blame) {
            throw new BlameException(packet.signer, packet);
        }

        return packet.message;
    }

    // Wait to receive a message from a given player.
    public Message receiveFromBlameless(VerificationKey from, Phase expectedPhase)
            throws WaitingException, InterruptedException, IOException {

        Packet packet;
        do {
            packet = receiveNextPacket(expectedPhase);
            if (packet == null) throw new WaitingException(from);

        } while (expectedPhase != Phase.Blame && packet.phase == Phase.Blame);

        return packet.message;
    }

    // Receive messages from a set of players, which may come in any order.
    private Map<VerificationKey, Message> receiveFromMultiple(
            Set<VerificationKey> from,
            Phase expectedPhase,
            boolean ignoreBlame // Whether to stop if a blame message is received.
    ) throws InterruptedException, IOException, WaitingException, BlameException {

        // Collect the messages in here.
        Map<VerificationKey, Packet> broadcasts = new HashMap<>();

        // Don't receive a message from myself.
        from.remove(sk.VerificationKey());

        while (from.size() > 0) {
            Packet packet = receiveNextPacket(expectedPhase);
            if (packet == null) throw new WaitingException(from);

            if (expectedPhase != Phase.Blame && packet.phase == Phase.Blame) {
                if (!ignoreBlame) {
                    // Put the messages already collected back so that they can be received later.
                    for (Packet p : broadcasts.values()) {
                        delivered.add(p);
                    }

                    throw new BlameException(packet.signer, packet);
                }
                continue;
            }
            VerificationKey sender = packet.signer;

            if (broadcasts.containsKey(sender)) {
                throw new ProtocolException();
            }
            broadcasts.put(sender, packet);
            from.remove(sender);
        }

        // Strip the messages of signatures and routing information.
        Map<VerificationKey, Message> messages = new HashMap<>();

        for (Map.Entry<VerificationKey, Packet> packet : broadcasts.entrySet()) {
            messages.put(packet.getKey(), packet.getValue().message);
        }

        return messages;
    }

    public Map<VerificationKey, Message> receiveFromMultiple(
            Set<VerificationKey> from,
            Phase expectedPhase
    )
            throws InterruptedException, BlameException, WaitingException, IOException {

        return receiveFromMultiple(from, expectedPhase, false);
    }

    // Sometimes we want to finish what we're doing before handling any blame message.
    public Map<VerificationKey, Message> receiveFromMultipleBlameless(
            Set<VerificationKey> from,
            Phase expectedPhase
    ) throws InterruptedException, WaitingException, IOException {

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
    public Map<VerificationKey, Queue<Packet>> receiveAllBlame()
            throws InterruptedException, IOException {
        Map<VerificationKey, Queue<Packet>> blame = new HashMap<>();
        for (VerificationKey player : players) {
            blame.put(player, new LinkedList<Packet>());
        }

        // First get the blame messages in history.
        for (Packet packet : history) {
            if (packet.phase == Phase.Blame) {
                blame.get(packet.signer).add(packet);
            }
        }

        // Then receive any more blame messages until there are no more.
        while (true) {
            Packet next = receiveNextPacket(Phase.Blame);
            if (next == null) break;

            blame.get(next.signer).add(next);
        }

        return blame;
    }
}
