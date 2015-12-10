package com.shuffle.form;

/**
 * Created by Daniel Krawisz on 12/9/15.
 */
public class MockCoinSignature implements CoinSignature {
    CoinTransaction t;
    MockVerificationKey key;

    MockCoinSignature(CoinTransaction t, MockVerificationKey key) {
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
}
