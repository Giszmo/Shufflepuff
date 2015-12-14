package com.shuffle.form;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Operations relating to the verification and formatting of messages when they are sent or received.
 *
 * Created by Daniel Krawisz on 12/7/15.
 */
class NetworkOperations {
    SessionIdentifier τ; // The session identifier of this protocol.
    Network network; // The connection to the shuffle network.
    ShuffleMachine machine; // The machine running the protocol.
    SigningKey sk;
    int N; // the number of players.
    VerificationKey players[]; // The current players.

    NetworkOperations(SessionIdentifier τ, SigningKey sk, VerificationKey players[], Network network, ShuffleMachine machine) {
        this.τ = τ;
        this.network = network;
        this.machine = machine;
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

    VerificationKey determineSender(Packet packet) throws CryptographyException, InvalidImplementationException, FormatException, ValueException {
        Set<VerificationKey> keys = opponentSet();

        for(VerificationKey key : keys) {
            if(key.readSignature(packet)) {
                return key;
            }
        }

        throw new ValueException(ValueException.Values.sender);
    }

    private void prepareForSending(Packet packet) throws CryptographyException, InvalidImplementationException {
        packet.append(machine.currentPhase());
        packet.append(τ);
        sk.sign(packet);
    }

    public void broadcast(Packet packet) throws TimeoutException, CryptographyException, InvalidImplementationException {
        Set<VerificationKey> keys = opponentSet();

        prepareForSending(packet);

        for (VerificationKey key : keys) {
            network.sendTo(key, packet);
        }
    }

    public void sendTo(VerificationKey to, Packet packet) throws TimeoutException, CryptographyException, InvalidImplementationException {
        prepareForSending(packet);

        network.sendTo(to, packet);
    }

    public Packet receiveFrom(VerificationKey from) throws TimeoutException, CryptographyException, FormatException, ValueException, InvalidImplementationException, BlameReceivedException, InterruptedException {
        Packet packet = network.receive();

        // If we receive a message, but it is not from the expected source, it might be a blame message.
        if (!from.readSignature(packet)) {
            VerificationKey sender = determineSender(packet);

            // Sender is valid. What about the rest of the message?
            if (!τ.equals(packet.removeSessionIdentifier())) {
                throw new ValueException(ValueException.Values.τ,τ.toString(),packet.removeSessionIdentifier().toString());
            }

            ShufflePhase phase = packet.removeShufflePhase();
            if (phase == ShufflePhase.Blame) {
                // It seems that one of the other players is blaming another for being a cheater!
                // How curious. Let's find out what happened.
                throw new BlameReceivedException(sender, packet);
            }

            throw new ValueException(ValueException.Values.phase,phase.toString(), machine.currentPhase().toString());
        }

        // Check that this is someone in the same round of this protocol as us.
        if (!τ.equals(packet.removeSessionIdentifier())) {
            throw new ValueException(ValueException.Values.τ,τ.toString(),packet.removeSessionIdentifier().toString());
        }

        // Check that the message phase is correct.
        ShufflePhase phase = packet.removeShufflePhase();
        if (machine.currentPhase() != phase) {
            throw new ValueException(ValueException.Values.phase,phase.toString(), machine.currentPhase().toString());
        }

        return packet;
    }

    public Map<VerificationKey, Packet> receiveFromMultiple(Set<VerificationKey> from) throws TimeoutException, CryptographyException, FormatException, InvalidImplementationException, ValueException, BlameReceivedException, InterruptedException {
        Map<VerificationKey, Packet> broadcasts = new HashMap<>();

        while (from.size() > 0) {
            Packet packet = network.receive();
            VerificationKey sender = determineSender(packet);

            // Check session identifier.
            if (!τ.equals(packet.removeSessionIdentifier())) {
                throw new ValueException(ValueException.Values.τ,τ.toString(),packet.removeSessionIdentifier().toString());
            }

            // Check that the message phase is correct.
            ShufflePhase phase = packet.removeShufflePhase();
            if (phase == ShufflePhase.Blame) {
                // Someone is blaming someone else for bad behavior!
                throw new BlameReceivedException(sender, packet);
            }
            if (machine.currentPhase() != phase) {
                throw new ValueException(ValueException.Values.phase,phase.toString(), machine.currentPhase().toString());
            }

            broadcasts.put(sender, packet);
            from.remove(sender);
        }

        return broadcasts;
    }
}
