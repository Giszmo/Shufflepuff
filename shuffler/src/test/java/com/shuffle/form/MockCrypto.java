package com.shuffle.form;

/**
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MockCrypto implements Crypto {
    @Override
    public DecryptionKey DecryptionKey() throws CryptographyException {
        return null;
    }

    @Override
    public SigningKey SigningKey() throws CryptographyException {
        return null;
    }

    @Override
    public int getRandom(int n) throws CryptographyException {
        return 0;
    }

    @Override
    public Message hash(Message m) throws CryptographyException {
        return null;
    }
}
