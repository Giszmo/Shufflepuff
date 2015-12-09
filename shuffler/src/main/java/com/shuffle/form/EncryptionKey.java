package com.shuffle.form;

/**
 * Created by Daniel Krawisz on 12/4/15.
 */
public interface EncryptionKey {
    void encrypt(Packet m) throws CryptographyException;
}
