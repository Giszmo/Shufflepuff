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

    Queue<Message> received = new LinkedList<>(); // A queue of messages that has been reived that we aren't ready to look at yet.

    NetworkOperations(SessionIdentifier τ, SigningKey sk, VerificationKey players[], Network network) {
        this.τ = τ;
        this.network = network;
        this.sk = sk;
        this.players = players;
        this.N = players.length;
    }

    // Get the set of players other than myself from i to N.
    public Set<VerificationKey> opponentSet(int i, int n) throws CryptographyException, InvalidImplementationException {
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

    public Set<VerificationKey> opponentSet() throws CryptographyException, InvalidImplementationException {
        return opponentSet(1, N);
    }

    public void broadcast(Message message) throws TimeoutException, CryptographyException, InvalidImplementationException {
        Set<VerificationKey> keys = opponentSet();

        for (VerificationKey key : keys) {
            network.sendTo(key, message);
        }
    }

    public void sendTo(VerificationKey to, Message message) throws TimeoutException, CryptographyException, InvalidImplementationException {
        network.sendTo(to, message);
    }

    // Get the next message from the phase we're in. It's possible for other players to get
    // ahead under some circumstances, so we have to keep their messages to look at later.
    Message getNextMessage(ShufflePhase expectedPhase)
            throws FormatException, CryptographyException, BlameReceivedException,
            InterruptedException, TimeoutException, InvalidImplementationException, ValueException {

        // Go through the queue of received messages if any are there.
        if (received.size() > 0) {
            Iterator<Message> i = received.iterator();
            while (i.hasNext()) {
                Message message = i.next();

                // Return any that matches what we're looking for.
                if (expectedPhase == message.getShufflePhase()) {
                    i.remove();
                    return message;
                }
            }
        }

        // Now we wait for the right message from the network, since we haven't already received it.
        while (true) {
            Message message = network.receive();
            VerificationKey sender = message.getSigner();
            ShufflePhase phase = message.getShufflePhase();

            // Check that this is someone in the same session of this protocol as us.
            if (!τ.equals(message.getSessionIdentifier())) {
                throw new ValueException(ValueException.Values.τ, τ.toString(), message.getSessionIdentifier().toString());
            }

            if (expectedPhase == phase) {
                return message;
            }

            if (phase == ShufflePhase.Blame) {
                throw new BlameReceivedException(sender, message);
            }

            received.add(message);
        }
    }

    public Message receiveFrom(VerificationKey from, ShufflePhase expectedPhase)
            throws TimeoutException, CryptographyException, FormatException, ValueException,
            InvalidImplementationException, com.shuffle.protocol.BlameReceivedException, InterruptedException {

        Message message = getNextMessage(expectedPhase);
        VerificationKey sender = message.getSigner();

        // If we receive a message, but it is not from the expected source, it might be a blame message.
        if (!from.equals(sender)) {
            throw new ValueException(ValueException.Values.phase, message.getShufflePhase().toString(), expectedPhase.toString());
        }

        return message;
    }

    public Map<VerificationKey, Message> receiveFromMultiple(Set<VerificationKey> from, ShufflePhase expectedPhase)
            throws TimeoutException, CryptographyException, FormatException,
            InvalidImplementationException,ValueException, BlameReceivedException, InterruptedException {

        // Collect the messages in here.
        Map<VerificationKey, Message> broadcasts = new HashMap<>();

        while (from.size() > 0) {
            Message message = getNextMessage(expectedPhase);
            VerificationKey sender = message.getSigner();

            broadcasts.put(sender, message);
            from.remove(sender);
        }

        return broadcasts;
    }
}
