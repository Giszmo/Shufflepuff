package com.shuffle.protocol;

import java.util.LinkedList;
import java.util.Queue;
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

    MockCrypto(int seed) {
        this.rand = null;
        signingKeyCounter = 1;
        decryptionKeyCounter = 1;

        notCryptographicallySecure = new Random(seed);
    }

    MockCrypto(MockRandomSequence rand) {
        this.rand = rand;
        signingKeyCounter = 1;
        decryptionKeyCounter = 1;
    }

    MockCrypto setSigningKeyCounter(int count) {
        signingKeyCounter = count;
        return this;
    }

    @Override
    public synchronized DecryptionKey DecryptionKey() throws CryptographyError {
        return new MockDecryptionKey(decryptionKeyCounter++);
    }

    @Override
    public synchronized SigningKey SigningKey() throws CryptographyError {
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

        MockMessage p = ((MockMessage)m).copy();

        Queue<MockMessage.Atom> z = new LinkedList<>();

        z.add(new MockMessage.Atom(new MockMessage.Hash(p.atoms)));

        p.atoms = z;

        return p;
    }
}
