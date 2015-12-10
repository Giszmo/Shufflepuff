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
    boolean equals(Packet packet) throws InvalidImplementationException;

    Packet append(VerificationKey vk) throws InvalidImplementationException;
    Packet append(EncryptionKey vk) throws InvalidImplementationException;
    Packet append(CoinSignature sig) throws InvalidImplementationException;
    Packet append(ShufflePhase phase) throws InvalidImplementationException;
    Packet append(SessionIdentifier τ) throws InvalidImplementationException;
    Packet append(Packet packet) throws InvalidImplementationException, FormatException;


    SessionIdentifier removeSessionIdentifier() throws FormatException;
    ShufflePhase removeShufflePhase() throws FormatException;
    EncryptionKey removeEncryptionKey() throws FormatException, InvalidImplementationException;
    VerificationKey removeVerificationKey() throws FormatException, InvalidImplementationException;
    CoinSignature removeCoinSignature() throws FormatException, InvalidImplementationException;

    // Removes the next element of the packet. Returns null if the packet is empty.
    Packet poll();
}
