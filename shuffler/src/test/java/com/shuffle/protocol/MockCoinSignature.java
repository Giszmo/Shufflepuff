package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/9/15.
 */
public class MockCoinSignature implements Coin.CoinSignature {
    Coin.CoinTransaction t;
    MockVerificationKey key;

    MockCoinSignature(Coin.CoinTransaction t, MockVerificationKey key) {
        this.t = t;
        this.key = key;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MockCoinSignature)) {
            return false;
        }

        return (key.equals((MockCoinSignature)o));
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
