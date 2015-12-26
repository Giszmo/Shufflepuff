package com.shuffle.cryptocoin;

import com.shuffle.protocol.FormatException;

/**
 * TO BE IMPLEMENTED BY THE USER
 *
 * A private key used for decryption.
 *
 * Created by Daniel Krawisz on 12/4/15.
 */
public interface DecryptionKey {
    EncryptionKey EncryptionKey();
    Address decrypt(Address m) throws FormatException, CryptographyError;
}