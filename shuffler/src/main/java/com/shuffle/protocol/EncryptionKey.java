package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/4/15.
 */
public interface EncryptionKey {
    void encrypt(Message m) throws CryptographyException, InvalidImplementationException, FormatException;
}
