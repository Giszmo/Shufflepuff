package com.shuffle.form;

import java.util.LinkedList;
import java.util.Queue;

/**
 * TODO
 *
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MockCrypto implements Crypto {
    int signingKeyCounter;
    int decryptionKeyCounter;

    MockRandomSequence rand;

    MockCrypto() {
        this.rand = null;
        signingKeyCounter = 0;
        decryptionKeyCounter = 0;
    }

    MockCrypto(MockRandomSequence rand) {
        this.rand = rand;
        signingKeyCounter = 0;
        decryptionKeyCounter = 0;
    }

    @Override
    public DecryptionKey DecryptionKey() throws CryptographyException {
        return new MockDecryptionKey(decryptionKeyCounter++);
    }

    @Override
    public SigningKey SigningKey() throws CryptographyException {
        return new MockSigningKey(signingKeyCounter++);
    }

    @Override
    public int getRandom(int n) throws CryptographyException, InvalidImplementationException {
        if (rand == null) {
            throw new CryptographyException();
        }
        return rand.getRandom(n);
    }

    @Override
    public void hash(Packet m) throws CryptographyException, InvalidImplementationException {
        if (!(m instanceof MockPacket)) {
            throw new InvalidImplementationException();
        }

        MockPacket p = (MockPacket)m;

        Queue<MockPacket.Atom> atoms = new LinkedList<>();
        atoms.add(new MockPacket.Atom(new MockPacket.Hashed(p.atoms)));
    }
}
