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
    Packet append(VerificationKey vk) throws InvalidImplementationException;
    Packet append(EncryptionKey vk) throws InvalidImplementationException;
    Packet append(CoinSignature sig) throws InvalidImplementationException;
    Packet append(ShufflePhase phase) throws InvalidImplementationException;
    Packet append(SessionIdentifier τ) throws InvalidImplementationException;
    Packet append(Packet packet) throws InvalidImplementationException;

    boolean equal(Packet packet) throws InvalidImplementationException;

    // Strips the signature, verifies it, and returns the verification key of the signer.
    SessionIdentifier readSessionIdentifier();
    ShufflePhase readShufflePhase();

    // Removes the next element of the packet and attempt to interpret it as an Encryption key.
    EncryptionKey readEncryptionKey() throws FormatException, InvalidImplementationException;
    VerificationKey readVerificationKey() throws FormatException, InvalidImplementationException;
    CoinSignature readCoinSignature() throws FormatException, InvalidImplementationException;

    // Removes the next element of the packet. Returns null if the packet is empty.
    Packet poll();
}
