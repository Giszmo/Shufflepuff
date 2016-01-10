package com.shuffle.protocol;

import com.shuffle.cryptocoin.CryptographyError;
import com.shuffle.cryptocoin.Signature;
import com.shuffle.cryptocoin.SigningKey;
import com.shuffle.cryptocoin.Transaction;
import com.shuffle.cryptocoin.VerificationKey;

/**
 * Created by Daniel Krawisz on 12/9/15.
 */
public class MockSigningKey implements SigningKey {
    final int index;

    MockSigningKey(int index) {
        this.index = index;
    }

    @Override
    public VerificationKey VerificationKey() throws CryptographyError {
        return new MockVerificationKey(index);
    }

    @Override
    public Signature makeSignature(Transaction t) throws CryptographyError {
        return new MockSignature(t, new MockVerificationKey(index));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MockSigningKey)) {
            return false;
        }

        return index == ((MockSigningKey)o).index;
    }

    @Override
    public String toString() {
        return "sk[" + index + "]";
    }

    @Override
    public int hashCode() {
        return index;
    }

    /*@Override
    public int compareTo(Object o) {
        if(!(o instanceof MockSigningKey)) {
            return -1;
        }

        MockSigningKey key = ((MockSigningKey)o);

        if (index == key.index) {
            return 0;
        }

        if (index < key.index) {
            return 1;
        }

        return -1;
    }*/
}
