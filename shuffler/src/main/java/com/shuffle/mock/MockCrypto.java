package com.shuffle.mock;

import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.Message;

import java.util.Random;

/**
 *
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MockCrypto implements Crypto {
    int signingKeyCounter;
    int decryptionKeyCounter;

    MockRandomSequence rand;

    Random notCryptographicallySecure;

    public MockCrypto(int seed) {
        this.rand = null;
        signingKeyCounter = 1;
        decryptionKeyCounter = 1;

        notCryptographicallySecure = new Random(seed);
    }

    public MockCrypto(MockRandomSequence rand) {
        this.rand = rand;
        signingKeyCounter = 1;
        decryptionKeyCounter = 1;
    }

    MockCrypto setSigningKeyCounter(int count) {
        signingKeyCounter = count;
        return this;
    }

    @Override
    public synchronized DecryptionKey makeDecryptionKey() throws CryptographyError {
        return new MockDecryptionKey(decryptionKeyCounter++);
    }

    @Override
    public synchronized SigningKey makeSigningKey() throws CryptographyError {
        return new MockSigningKey(signingKeyCounter++);
    }

    @Override
    public synchronized int getRandom(int n) throws CryptographyError, InvalidImplementationError {
        if (rand == null) {
            return notCryptographicallySecure.nextInt(n + 1);
        }
        return rand.getRandom(n);
    }

    @Override
    public synchronized Message hash(Message m) throws CryptographyError, InvalidImplementationError {
        if (!(m instanceof MockMessage)) {
            throw new InvalidImplementationError();
        }

        MockMessage p = ((MockMessage)m);

        return new MockMessage().attach(new MockMessage.Hash(p.atoms));
    }
}
