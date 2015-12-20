package com.shuffle.protocol;

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
    public boolean verify(Coin.CoinTransaction t, CoinSignature sig) throws InvalidImplementationError {
        if (!(sig instanceof MockCoinSignature)) {
            throw new InvalidImplementationError();
        }

        return (((MockCoinSignature)sig).t.equals(t)) && (((MockCoinSignature)sig).key.equals(this));
    }

    @Override
    public boolean equals(Object vk) {
        if(!(vk instanceof MockVerificationKey)) {
            return false;
        }

        return index == ((MockVerificationKey)vk).index;
    }

    @Override
    public Coin.CoinAddress address() {
        return new MockCoinAddress(index);
    }

    public String toString() {
        return "vk[" + index + "]";
    }

    @Override
    public int hashCode() {
        return index;
    }
}
