package com.shuffle.protocol;

import com.shuffle.bitcoin.Signature;
import com.shuffle.bitcoin.Transaction;

/**
 * Created by Daniel Krawisz on 12/9/15.
 */
public class MockSignature implements Signature {
    Transaction t;
    MockVerificationKey key;

    MockSignature(Transaction t, MockVerificationKey key) {
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
