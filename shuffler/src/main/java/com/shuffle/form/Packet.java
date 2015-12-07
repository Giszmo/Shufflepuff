package com.shuffle.form;

import java.util.Set;

/**
 * TO BE IMPLEMENTED BY THE USER
 *
 * This interface should not be implemented until we know some more specification details of the
 * protocol that were left out of the original paper.
 *
 * Created by Daniel Krawisz on 12/6/15.
 */
public interface Packet {
    Packet append(VerificationKey vk);
    Packet append(EncryptionKey vk);
    Packet append(CoinSignature sig);
    Packet append(ShufflePhase phase);
    Packet append(SessionIdentifier τ);
    Packet append(Packet packet);

    boolean equal(Packet packet);

    // Produces a signed packet.
    Packet sign(SigningKey sk);

    // Strips the signature, verifies it, and returns the verification key of the signer.
    SessionIdentifier readSessionIdentifier();
    ShufflePhase readShufflePhase();

    // Removes the next element of the packet and attempt to interpret it as an Encryption key.
    EncryptionKey readEncryptionKey() throws FormatException;
    CoinSignature readCoinSignature() throws FormatException;

}
