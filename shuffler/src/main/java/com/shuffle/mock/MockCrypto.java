/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.mock;

import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.player.Message;
import com.shuffle.protocol.InvalidImplementationError;

/**
 *
 *
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MockCrypto implements Crypto {
    public interface Random {
        int getRandom(int n) throws CryptographyError, InvalidImplementationError;
    }

    private int signingKeyCounter;
    private int decryptionKeyCounter;

    private final Random rand;

    public MockCrypto(Random rand) {
        this.rand = rand;
        signingKeyCounter = 1;
        decryptionKeyCounter = 1;
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
        return rand.getRandom(n);
    }

    @Override
    public synchronized com.shuffle.protocol.message.Message hash(com.shuffle.protocol.message.Message m)
            throws CryptographyError, InvalidImplementationError {

        if (!(m instanceof Message)) {
            throw new InvalidImplementationError();
        }

        Message p = ((Message)m);

        return p.hashed();
    }
}
