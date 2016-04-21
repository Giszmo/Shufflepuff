package com.shuffle.mock;

/**
 * A class used for testing purposes that uses a standard RNG instead of one that is
 * cryptographically secure. s
 *
 * Created by Daniel Krawisz on 3/23/16.
 */
public class InsecureRandom implements MockCrypto.Random {
    private final java.util.Random r;

    public InsecureRandom(int seed) {
        this.r = new java.util.Random(seed);
    }

    @Override
    public int getRandom(int n) {
        return r.nextInt(n + 1);
    }
}
