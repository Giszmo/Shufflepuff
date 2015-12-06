package com.shuffle.form;

import com.shuffle.core.*;
import com.shuffle.core.MessageFactory;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Daniel Krawisz on 12/5/15.
 */
public class TestShuffleMachine {
    private class shuffleTestCase {
        int[] randomSequence;
        com.shuffle.core.Message input;
        com.shuffle.core.Message expected;
    }


    private ShuffleMachine uninitialized() {
        return new ShuffleMachine(
                new MockSessionIdentifier(),
                new VerificationKey[]{},
                new MessageFactory(),
                new MockCrypto(),
                new MockCoin(),
                new MockNetwork());
    }

    @Test
    // This tests some cases for the function that randomly shuffles a message.
    public void shuffleTest() {
        shuffleTestCase tests[] = new shuffleTestCase[]{};

        for(shuffleTestCase test : tests) {
            ShuffleMachine machine = uninitialized();

            try {
                machine.shuffle(test.input);
            } catch (CryptographyException e) {
                Assert.fail("Unexpected CryptographyException");
            } catch (InvalidImplementationException e) {
                Assert.fail("Unexpected InvalidImplementationException");
            } catch (FormatException e) {
                Assert.fail("Unexpected FormatException");
            }

            Assert.assertTrue(test.input.equal(test.expected));
        }
    }
}
