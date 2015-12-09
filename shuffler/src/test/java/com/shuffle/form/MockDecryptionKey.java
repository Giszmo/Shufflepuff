package com.shuffle.form;

/**
 * TODO
 *
 * Created by Daniel Krawisz on 12/8/15.
 */
public class MockDecryptionKey implements DecryptionKey {
    int index;

    @Override
    public EncryptionKey EncryptionKey() {
        return null;
    }

    @Override
    public boolean decrypt(Packet m) {
        return false;
    }
}
