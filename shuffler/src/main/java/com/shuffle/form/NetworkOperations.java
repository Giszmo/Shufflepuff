package com.shuffle.form;

import java.util.Map;
import java.util.Set;

/**
 * Operations relating to the verification and formatting of messages when they are sent or received.
 *
 * Created by Daniel Krawisz on 12/7/15.
 */
public class NetworkOperations {
    SessionIdentifier τ; // The session identifier of this protocol.
    Network network; // The connection to the shuffle network.
    ShuffleMachine machine; // The machine running the protocol.
    SigningKey sk;

    NetworkOperations(SessionIdentifier τ, SigningKey sk, Network network, ShuffleMachine machine) {
        this.τ = τ;
        this.network = network;
        this.machine = machine;
        this.sk = sk;
    }

    private void prepareForSending(Packet packet) {
        packet.append(machine.currentPhase());
        packet.append(τ);
        packet.sign(sk);
    }

    private VerificationKey determineSender(Packet packet) throws CryptographyException, FormatException {
        Set<VerificationKey> keys = machine.opponentSet();

        for(VerificationKey key : keys) {
            if(key.readSignature(packet)) {
                return key;
            }
        }

        throw new FormatException();
    }

    public void broadcast(Packet packet) throws TimeoutException {
        Set<VerificationKey> keys = machine.opponentSet();

        prepareForSending(packet);

        for (VerificationKey key : keys) {
            network.sendTo(key, packet);
        }
    }

    public void sendTo(VerificationKey to, Packet packet) throws TimeoutException {
        prepareForSending(packet);

        network.sendTo(to, packet);
    }

    public Packet receiveFrom(VerificationKey from) throws TimeoutException, ProtocolAbortedException, CryptographyException, FormatException, ValueException, BlameException {
        Packet packet = network.receive();

        // If we receive a message, but it is not from the expected source, it might be a blame message.
        if (!from.readSignature(packet)) {
            VerificationKey sender = determineSender(packet);

            // Check that this is someone in the same round of this protocol as us.
            if (!τ.equals(packet.readSessionIdentifier())) {
                throw new ValueException();
            }

            // It seems that one of the other players is blaming another for being a cheater!
            // How curious. Let's find out what happened.
            if (machine.currentPhase() != ShufflePhase.Blame) {
                throw new BlameException(sender, packet);
            }
        }

        // Check that this is someone in the same round of this protocol as us.
        if (!τ.equals(packet.readSessionIdentifier())) {
            throw new ValueException();
        }

        // Check that the message phase is correct.
        if (machine.currentPhase() != packet.readShufflePhase()) {
            throw new ValueException();
        }

        return packet;
    }

    // TODO
    public Map<VerificationKey, Packet> receiveFrom(Set<VerificationKey> from) throws TimeoutException, ProtocolAbortedException {
        return null;
    }
}
