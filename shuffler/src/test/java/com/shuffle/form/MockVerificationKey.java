package com.shuffle.form;

/**
 * TODO
 *
 * Created by Daniel Krawisz on 12/7/15.
 */
public class MockVerificationKey implements VerificationKey {
    int identity;

    public MockVerificationKey(int identity) {
        this.identity = identity;
    }


    // These functions are not implemented yet.
    @Override
    public boolean verify(CoinTransaction t, CoinSignature sig) throws InvalidImplementationException {
        throw new InvalidImplementationException();
    }

    @Override
    public boolean equals(VerificationKey vk) throws InvalidImplementationException {
        if(!(vk instanceof MockVerificationKey)) {
            throw new InvalidImplementationException();
        }

        return identity == ((MockVerificationKey)vk).identity;
    }

    @Override
    public boolean readSignature(Packet packet) throws CryptographyException, FormatException, InvalidImplementationException {
        if (!(packet instanceof MockPacket)) {
            throw new InvalidImplementationException();
        }

        return identity == ((MockPacket)packet).from.identity;
    }
}
