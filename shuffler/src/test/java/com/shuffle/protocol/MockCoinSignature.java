package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/9/15.
 */
public class MockCoinSignature implements CoinSignature {
    Coin.CoinTransaction t;
    MockVerificationKey key;

    MockCoinSignature(Coin.CoinTransaction t, MockVerificationKey key) {
        this.t = t;
        this.key = key;
    }

    @Override
    public boolean equals(CoinSignature sig) throws InvalidImplementationException {
        if (!(sig instanceof MockCoinSignature)) {
            throw new InvalidImplementationException();
        }

        return (key.equals((MockCoinSignature)sig));
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
