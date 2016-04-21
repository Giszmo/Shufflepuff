package com.shuffle.mock;

import com.shuffle.protocol.InvalidImplementationError;

/**
 * Created by Daniel Krawisz on 12/6/15.
 */
public class RandomSequence implements MockCrypto.Random {
    private int counter = 0;
    public final int[] sequence;

    public RandomSequence(int[] sequence) {
        this.sequence = sequence;
    }

    @Override
    public int getRandom(int n) {
        // we use a premature end of the sequence blockchain simulate a problem.
        if (counter >= sequence.length) {
            return 0;
        }

        // Tests should be designed so as not blockchain give invalid numbers.
        if (sequence[counter] > n || sequence[counter] < 0) {
            throw new InvalidImplementationError();
        }

        return sequence[counter++];
    }
}
