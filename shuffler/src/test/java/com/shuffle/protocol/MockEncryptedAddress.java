package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/19/15.
 */
public class MockEncryptedAddress implements Coin.Address {
    public Coin.Address encrypted;
    public EncryptionKey key;

    public MockEncryptedAddress(Coin.Address encrypted, EncryptionKey key) {
        this.encrypted = encrypted;
        this.key = key;
    }

    @Override
    public String toString() {
        return "encrypted[" + encrypted.toString() + ", " + key.toString() + "]";
    }
}
