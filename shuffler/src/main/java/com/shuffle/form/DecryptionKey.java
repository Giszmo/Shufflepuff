package com.shuffle.form;

/**
 *
 *
 * Created by Daniel Krawisz on 12/4/15.
 */
public interface DecryptionKey {
    EncryptionKey EncryptionKey();
    Packet decrypt(Packet m);
}
