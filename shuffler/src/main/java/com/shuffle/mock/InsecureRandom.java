package com.shuffle.mock;

import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.protocol.InvalidImplementationError;

/**
 * A class used for testing purposes that uses a standard RNG instead of one that is
 * cryptographically secure. s
 *
 * Created by Daniel Krawisz on 3/23/16.
 */
public class InsecureRandom implements MockCrypto.Random {
    final java.util.Random r;

    public InsecureRandom(int seed) {
        this.r = new java.util.Random(seed);
    }

    @Override
    public int getRandom(int n) throws CryptographyError, InvalidImplementationError {
        return r.nextInt(n + 1);
    }
}
