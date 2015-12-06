package com.shuffle.form;

/**
 * Created by Daniel Krawisz on 12/6/15.
 */
public class MockRandomSequence {
    int counter = 0;
    int sequence[];

    MockRandomSequence(int sequence[]) {
        this.sequence = sequence;
    }

    int getRandom(int n) throws CryptographyException, InvalidImplementationException {
        // we use a premature end of the sequence to simulate a problem.
        if (counter >= sequence.length) {
            throw new CryptographyException();
        }

        // Tests should be designed so as not to give invalid numbers.
        if (sequence[counter] > n || sequence[counter] < 0) {
            throw new InvalidImplementationException();
        }

        return sequence[counter++];
    }
}
