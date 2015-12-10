package com.shuffle.form;

/**
 * TODO
 *
 * Created by Daniel Krawisz on 12/7/15.
 */
public class MockVerificationKey implements VerificationKey {
    int index;

    public MockVerificationKey(int index) {
        this.index = index;
    }

    // These functions are not implemented yet.
    @Override
    public boolean verify(CoinTransaction t, CoinSignature sig) throws InvalidImplementationException {
        if (!(sig instanceof MockCoinSignature)) {
            throw new InvalidImplementationException();
        }

        return (((MockCoinSignature)sig).t.equals(t)) && (((MockCoinSignature)sig).key.equals(this));
    }

    @Override
    public boolean equals(VerificationKey vk) throws InvalidImplementationException {
        if(!(vk instanceof MockVerificationKey)) {
            throw new InvalidImplementationException();
        }

        return index == ((MockVerificationKey)vk).index;
    }

    @Override
    public boolean readSignature(Packet packet) throws CryptographyException, FormatException, InvalidImplementationException {
        if (!(packet instanceof MockPacket)) {
            throw new InvalidImplementationException();
        }

        return equals(((MockPacket)packet).signer);
    }

    public String toString() {
        return "MockVerificationKey[" + index + "]";
    }
}
