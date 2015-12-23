package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/9/15.
 */
public class MockSignature implements Coin.Signature {
    Coin.Transaction t;
    MockVerificationKey key;

    MockSignature(Coin.Transaction t, MockVerificationKey key) {
        this.t = t;
        this.key = key;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MockSignature)) {
            return false;
        }

        return (key.equals((MockSignature)o));
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
