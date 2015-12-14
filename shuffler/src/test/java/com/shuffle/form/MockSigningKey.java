package com.shuffle.form;

/**
 * Created by Daniel Krawisz on 12/9/15.
 */
public class MockSigningKey implements SigningKey {
    int index;
    MockVerificationKey key;

    MockSigningKey(int index) {
        this.index = index;
        key = new MockVerificationKey(index);
    }

    @Override
    public VerificationKey VerificationKey() throws CryptographyException {
        return key;
    }

    @Override
    public CoinSignature makeSignature(CoinTransaction t) throws CryptographyException {
        return new MockCoinSignature(t, key);
    }

    @Override
    public void sign(Packet packet) throws CryptographyException, InvalidImplementationException {
        if (!(packet instanceof MockPacket)) {
            throw new InvalidImplementationException();
        }

        if (((MockPacket)packet).signer == null) {
            throw new CryptographyException();
        }

        ((MockPacket)packet).signer = key;
    }
}
