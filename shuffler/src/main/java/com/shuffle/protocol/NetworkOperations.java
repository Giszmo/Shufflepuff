package com.shuffle.protocol;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.HashSet;

/**
 * Operations relating to the verification and formatting of inbox when they are sent or received.
 *
 * Created by Daniel Krawisz on 12/7/15.
 */
class NetworkOperations {
    SessionIdentifier τ; // The session identifier of this protocol.
    Network network; // The connection to the shuffle network.
    SigningKey sk;
    int N; // the number of players.
    VerificationKey players[]; // The current players.

    Queue<Packet> received = new LinkedList<>(); // A queue of messages that has been reived that we aren't ready to look at yet.

    NetworkOperations(SessionIdentifier τ, SigningKey sk, VerificationKey players[], Network network) {
        this.τ = τ;
        this.network = network;
        this.sk = sk;
        this.players = players;
        this.N = players.length;
    }

    // Get the set of players other than myself from i to N.
    public Set<VerificationKey> opponentSet(int i, int n) throws CryptographyError, InvalidImplementationError {
        if (i < 1) {
            i = 1;
        }
        Set<VerificationKey> set = new HashSet<>();
        for(int j = i; j <= n; j ++) {
            if (j > N) {
                return set;
            }

            if (!sk.VerificationKey().equals(players[j - 1])) {
                set.add(players[j - 1]);
            }
        }

        return set;
    }

    public Set<VerificationKey> opponentSet() throws CryptographyError, InvalidImplementationError {
        return opponentSet(1, N);
    }

    public void broadcast(Packet packet) throws TimeoutError, CryptographyError, InvalidImplementationError {
        Set<VerificationKey> keys = opponentSet();

        for (VerificationKey key : keys) {
            network.sendTo(key, packet);
        }
    }

    public void sendTo(VerificationKey to, Packet packet) throws TimeoutError, CryptographyError, InvalidImplementationError {
        network.sendTo(to, packet);
    }

    // Get the next message from the phase we're in. It's possible for other players to get
    // ahead under some circumstances, so we have to keep their messages to look at later.
    Packet getNextMessage(ShufflePhase expectedPhase)
            throws FormatException, CryptographyError, BlameReceivedException,
            InterruptedException, TimeoutError, InvalidImplementationError, ValueException {

        // Go through the queue of received messages if any are there.
        if (received.size() > 0) {
            Iterator<Packet> i = received.iterator();
            while (i.hasNext()) {
                Packet packet = i.next();

                // Return any that matches what we're looking for.
                if (expectedPhase == packet.phase) {
                    i.remove();
                    return packet;
                }
            }
        }

        // Now we wait for the right message from the network, since we haven't already received it.
        while (true) {
            Packet packet = network.receive();
            VerificationKey sender = packet.signer;
            ShufflePhase phase = packet.phase;

            // Check that this is someone in the same session of this protocol as us.
            if (!τ.equals(packet.τ)) {
                throw new ValueException(ValueException.Values.τ, τ.toString(), packet.τ.toString());
            }

            if (expectedPhase == phase) {
                return packet;
            }

            if (phase == ShufflePhase.Blame) {
                throw new BlameReceivedException(sender, packet);
            }

            received.add(packet);
        }
    }

    public Message receiveFrom(VerificationKey from, ShufflePhase expectedPhase)
            throws TimeoutError, CryptographyError, FormatException, ValueException,
            InvalidImplementationError, com.shuffle.protocol.BlameReceivedException, InterruptedException {

        Packet packet = getNextMessage(expectedPhase);

        // If we receive a message, but it is not from the expected source, it might be a blame message.
        if (!from.equals(packet.signer)) {
            throw new ValueException(ValueException.Values.phase, packet.phase.toString(), expectedPhase.toString());
        }

        return packet.message;
    }

    public Map<VerificationKey, Message> receiveFromMultiple(Set<VerificationKey> from, ShufflePhase expectedPhase)
            throws TimeoutError, CryptographyError, FormatException,
            InvalidImplementationError,ValueException, BlameReceivedException, InterruptedException {

        // Collect the messages in here.
        Map<VerificationKey, Message> broadcasts = new HashMap<>();

        while (from.size() > 0) {
            Packet packet = getNextMessage(expectedPhase);
            VerificationKey sender = packet.signer;

            broadcasts.put(sender, packet.message);
            from.remove(sender);
        }

        return broadcasts;
    }
}
