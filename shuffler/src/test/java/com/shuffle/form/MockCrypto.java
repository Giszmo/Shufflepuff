package com.shuffle.form;

/**
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MockCrypto implements Crypto {
    MockRandomSequence rand;

    MockCrypto(MockRandomSequence rand) {
        this.rand = rand;
    }

    @Override
    public DecryptionKey DecryptionKey() throws CryptographyException {
        return null;
    }

    @Override
    public SigningKey SigningKey() throws CryptographyException {
        return null;
    }

    @Override
    public int getRandom(int n) throws CryptographyException, InvalidImplementationException {
        return rand.getRandom(n);
    }

    @Override
    public Message hash(Message m) throws CryptographyException {
        return null;
    }
}
