package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/19/15.
 */
public class MockEncryptedCoinAddress implements Coin.CoinAddress {
    public Coin.CoinAddress encrypted;
    public EncryptionKey key;

    public MockEncryptedCoinAddress(Coin.CoinAddress encrypted, EncryptionKey key) {
        this.encrypted = encrypted;
        this.key = key;
    }

    @Override
    public String toString() {
        return "encrypted[" + encrypted.toString() + ", " + key.toString() + "]";
    }
}
