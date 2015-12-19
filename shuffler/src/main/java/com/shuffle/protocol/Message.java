package com.shuffle.protocol;

/**
 * TO BE IMPLEMENTED BY THE USER
 *
 * This interface should not be implemented until we know some more specification details of the
 * protocol that were left out of the original paper.
 *
 * Created by Daniel Krawisz on 12/6/15.
 */
public interface Message {

    Message attach(VerificationKey vk) throws InvalidImplementationException;
    Message attach(EncryptionKey vk) throws InvalidImplementationException;
    Message attach(CoinSignature sig) throws InvalidImplementationException;
    Message attach(Message message) throws InvalidImplementationException, FormatException;

    EncryptionKey removeEncryptionKey() throws FormatException, InvalidImplementationException;
    VerificationKey removeVerificationKey() throws FormatException, InvalidImplementationException;
    CoinSignature removeCoinSignature() throws FormatException, InvalidImplementationException;

    SessionIdentifier getSessionIdentifier() throws FormatException;
    ShufflePhase getShufflePhase() throws FormatException;
    VerificationKey getSigner() throws FormatException, CryptographyException;

    // Removes the next element of the packet. Returns null if the packet is empty.
    Message poll();
}
