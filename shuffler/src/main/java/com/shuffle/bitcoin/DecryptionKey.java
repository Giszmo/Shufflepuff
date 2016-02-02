package com.shuffle.bitcoin;

import com.shuffle.player.Marshaller;
import com.shuffle.protocol.FormatException;

/**
 *
 * A private key used for decryption.
 *
 * Created by Daniel Krawisz on 12/4/15.
 */
public interface DecryptionKey {
    EncryptionKey EncryptionKey();
    Address decrypt(Address m) throws FormatException, CryptographyError;
}