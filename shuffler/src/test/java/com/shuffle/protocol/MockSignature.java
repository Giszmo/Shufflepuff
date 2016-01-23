package com.shuffle.protocol;

import com.shuffle.bitcoin.Signature;
import com.shuffle.bitcoin.Transaction;

/**
 * Created by Daniel Krawisz on 12/9/15.
 */
public class MockSignature implements Signature {
    final Transaction t;
    final Packet packet;
    final MockVerificationKey key;

    MockSignature(Transaction t, MockVerificationKey key) {
        this.t = t;
        this.key = key;
        this.packet = null;
    }

    MockSignature(Packet packet, MockVerificationKey key) {
        this.packet = packet;
        this.key = key;
        this.t = null;
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
