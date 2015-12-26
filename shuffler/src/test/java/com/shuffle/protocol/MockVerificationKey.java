package com.shuffle.protocol;

import com.shuffle.cryptocoin.Address;
import com.shuffle.cryptocoin.Signature;
import com.shuffle.cryptocoin.Transaction;
import com.shuffle.cryptocoin.VerificationKey;

/**
 * TODO
 *
 * Created by Daniel Krawisz on 12/7/15.
 */
public class MockVerificationKey implements VerificationKey {
    final int index;

    public MockVerificationKey(int index) {
        this.index = index;
    }

    // These functions are not implemented yet.
    @Override
    public boolean verify(Transaction t, Signature sig) throws InvalidImplementationError {
        if (!(sig instanceof MockSignature)) {
            throw new InvalidImplementationError();
        }

        return (((MockSignature)sig).t.equals(t)) && (((MockSignature)sig).key.equals(this));
    }

    @Override
    public boolean equals(Object vk) {
        if(!(vk instanceof MockVerificationKey)) {
            return false;
        }

        return index == ((MockVerificationKey)vk).index;
    }

    @Override
    public Address address() {
        return new MockAddress(index);
    }

    public String toString() {
        return "vk[" + index + "]";
    }

    @Override
    public int hashCode() {
        return index;
    }

    @Override
    public int compareTo(Object o) {
        if(!(o instanceof MockVerificationKey)) {
            return -1;
        }

        MockVerificationKey key = ((MockVerificationKey)o);

        if (index == key.index) {
            return 0;
        }

        if (index < key.index) {
            return 1;
        }

        return -1;
    }
}
