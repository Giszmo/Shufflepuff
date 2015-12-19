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
    public synchronized DecryptionKey DecryptionKey() throws CryptographyException {
        return new MockDecryptionKey(decryptionKeyCounter++);
    }

    @Override
    public synchronized SigningKey SigningKey() throws CryptographyException {
        return new MockSigningKey(signingKeyCounter++);
    }

    @Override
    public synchronized int getRandom(int n) throws CryptographyException, InvalidImplementationException {
        if (rand == null) {
            return notCryptographicallySecure.nextInt(n + 1);
        }
        return rand.getRandom(n);
    }

    @Override
    public synchronized void hash(Message m) throws CryptographyException, InvalidImplementationException {
        if (!(m instanceof MockMessage)) {
            throw new InvalidImplementationException();
        }

        MockMessage p = (MockMessage)m;

        Queue<MockMessage.Atom> atoms = new LinkedList<>();
        atoms.add(new MockMessage.Atom(new MockMessage.Hash(p.atoms)));
    }
}
