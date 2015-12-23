package com.shuffle.protocol;

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
    public Coin.Signature makeSignature(Coin.Transaction t) throws CryptographyError {
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
}
