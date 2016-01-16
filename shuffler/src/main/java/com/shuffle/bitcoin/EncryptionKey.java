package com.shuffle.bitcoin;

/**
 *
 * A public encryption key.
 *
 * Created by Daniel Krawisz on 12/4/15.
 */
public interface EncryptionKey {
    Address encrypt(Address m) throws CryptographyError;
}
