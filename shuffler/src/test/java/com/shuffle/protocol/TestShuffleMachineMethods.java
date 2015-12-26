package com.shuffle.protocol;

import com.shuffle.cryptocoin.Address;
import com.shuffle.cryptocoin.Crypto;
import com.shuffle.cryptocoin.CryptographyError;
import com.shuffle.cryptocoin.DecryptionKey;
import com.shuffle.cryptocoin.SigningKey;
import com.shuffle.cryptocoin.VerificationKey;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * Tests the methods in the shuffle machine other than the main ones.
 *
 * Created by Daniel Krawisz on 12/5/15.
 */
public class TestShuffleMachineMethods {
    private class shuffleTestCase {
        int[] randomSequence;
        int[] input;
        int[] expected;

        shuffleTestCase(int[] randomSequence, int[] input, int[] expected) {
            this.randomSequence = randomSequence;
            this.input = input;
            this.expected = expected;
        }
    }

    private ShuffleMachine shuffleTestInitialization(SessionIdentifier τ, int rand[]) {
        return new ShuffleMachine(τ,
                new MockMessageFactory(),
                new MockCrypto(
                        new MockRandomSequence(rand)),
                new MockCoin(),
                new MockNetwork());
    }

    @Test
    // This tests some cases for the function that randomly shuffles a message.
    public void shuffleTest() {
        shuffleTestCase tests[] = new shuffleTestCase[]{
                // The empty case, of course!
                new shuffleTestCase(
                        new int[]{},
                        new int[]{},
                        new int[]{}
                ),
                // One element.
                new shuffleTestCase(
                        new int[]{0},
                        new int[]{1},
                        new int[]{1}
                ),
                // All possibilities for two elements.
                new shuffleTestCase(
                        new int[]{1, 0},
                        new int[]{1, 2},
                        new int[]{2, 1}
                ),
                new shuffleTestCase(
                        new int[]{0, 0},
                        new int[]{1, 2},
                        new int[]{1, 2}
                ),
                // Most possibilities for three elements.
                new shuffleTestCase(
                        new int[]{2, 0, 0},
                        new int[]{1, 2, 3},
                        new int[]{3, 1, 2}
                ),
                new shuffleTestCase(
                        new int[]{2, 1, 0},
                        new int[]{1, 2, 3},
                        new int[]{3, 2, 1}
                ),
                new shuffleTestCase(
                        new int[]{1, 1, 0},
                        new int[]{1, 2, 3},
                        new int[]{2, 1, 3}
                ),
                new shuffleTestCase(
                        new int[]{0, 1, 0},
                        new int[]{1, 2, 3},
                        new int[]{1, 3, 2}
                ),
                // Some randomly generated larger cases.
                new shuffleTestCase(
                        new int[]{1, 0, 1, 0},
                        new int[]{1, 2, 3, 4},
                        new int[]{2, 3, 1, 4}
                ),
                new shuffleTestCase(
                        new int[]{2, 3, 1, 0, 0},
                        new int[]{1, 2, 3, 4, 5},
                        new int[]{3, 2, 5, 1, 4}
                ),
                new shuffleTestCase(
                        new int[]{3, 2, 0, 0, 1, 0},
                        new int[]{1, 2, 3, 4, 5, 6},
                        new int[]{4, 1, 2, 3, 6, 5}
                ),
        };

        for(shuffleTestCase test : tests) {
            MockCrypto crypto = new MockCrypto(90);
            SigningKey key = null;
            try {
                key = crypto.SigningKey();
            } catch (CryptographyError e) {}
            SessionIdentifier τ = new MockSessionIdentifier();
            ShuffleMachine machine = shuffleTestInitialization(τ, test.randomSequence);

            ShufflePhase phase = ShufflePhase.Shuffling;

            Message input = new MockMessage();
            for (int i : test.input) {
                input.attach(new MockAddress(i));
            }
            Message expected = new MockMessage();
            for (int i : test.expected) {
                expected.attach(new MockAddress(i));
            }
            try {
                Message result = machine.shuffle(input);
                System.out.println("got " + result.toString() + "; expected " + expected.toString());
                Assert.assertTrue(result.equals(expected));
            } catch (CryptographyError e) {
                Assert.fail("Unexpected CryptographyException");
            } catch (InvalidImplementationError e) {
                Assert.fail("Unexpected InvalidImplementationException");
            } catch (FormatException e) {
                e.printStackTrace();
                Assert.fail("Unexpected FormatException: ");
            }
        }
    }

    private class areEqualTestCase {
        int[] input;
        boolean expected;

        areEqualTestCase(int[] input, boolean expected) {
            this.input = input;
            this.expected = expected;
        }
    }

    Map<VerificationKey, Message> mockPacketMap(int[] input) throws CryptographyError {
        Map<VerificationKey, Message> map = new HashMap<>();

        int index = 0;
        for(int i : input) {
            MockSigningKey key = new MockSigningKey(index);
            map.put(key.VerificationKey(), new MockMessage().attach(new MockAddress(i)));
            index++;
        }

        return map;
    }

    @Test
    public void testAreEqual() {
        areEqualTestCase tests[] = new areEqualTestCase[]{
                // The empty case, of course!
                new areEqualTestCase(
                        new int[]{},
                        true
                ),
                new areEqualTestCase(
                        new int[]{1},
                        true
                ),
                new areEqualTestCase(
                        new int[]{1, 1},
                        true
                ),
                new areEqualTestCase(
                        new int[]{1, 2},
                        false
                ),
                new areEqualTestCase(
                        new int[]{2, 2, 2},
                        true
                ),
                new areEqualTestCase(
                        new int[]{2, 3, 2},
                        false
                ),
                new areEqualTestCase(
                        new int[]{3, 2, 2},
                        false
                ),
                new areEqualTestCase(
                        new int[]{2, 2, 3},
                        false
                )
        };

        for(areEqualTestCase testCase : tests) {
            try {
                Assert.assertEquals(testCase.expected, ShuffleMachine.areEqual(mockPacketMap(testCase.input)));
            } catch (InvalidImplementationError e) {
                Assert.fail("Tests have failed due to error in test class.");
            } catch (CryptographyError e) {
                Assert.fail("Unexpected CryptographyExcption.");
            }
        }
    }

    private ShuffleMachine standardTestInitialization(SessionIdentifier τ, Crypto crypto) {
        return new ShuffleMachine(τ,
                new MockMessageFactory(),
                crypto,
                new MockCoin(),
                new MockNetwork());
    }

    @Test
    public void testDecryptAll() {
        MockCrypto crypto = new MockCrypto(56);
        MessageFactory messages = new MockMessageFactory();

        // Success cases.
        try {
            for(int i = 0; i <= 5; i++) {
                Message input = messages.make();
                Message output = messages.make();
                DecryptionKey dk = crypto.DecryptionKey();

                for (int j = 0; j <= i; j ++) {
                    Address addr = crypto.SigningKey().VerificationKey().address();

                    output.attach(addr);
                    input.attach(new MockEncryptedAddress(addr, dk.EncryptionKey()));
                }

                SessionIdentifier τ = new MockSessionIdentifier();
                ShuffleMachine machine = standardTestInitialization(τ, crypto);

                SigningKey sk = crypto.SigningKey();
                ShufflePhase phase = ShufflePhase.Shuffling;
                Message result = machine.decryptAll(new MockMessage().attach(input), dk, i);

                Assert.assertTrue(result.equals(output));
            }
        } catch (CryptographyError e) {
            Assert.fail("Unexpected CryptographyException:");
        } catch (FormatException e) {
            e.printStackTrace();
            Assert.fail("Unexpected FormatException:");
        } catch (InvalidImplementationError e) {
            Assert.fail("Unexpected InvalidImplementationException:");
        }

        // Fail cases.
        // TODO: include fail cases that lead to blame cases.
        try {
            for(int i = 0; i <= 5; i++) {
                MockSessionIdentifier τ = new MockSessionIdentifier();
                Message input = new MockMessage();
                DecryptionKey key = null;
                try {
                    key = crypto.DecryptionKey();

                    for (int j = 0; j <= i; j++) {

                        input.attach(new MockEncryptedAddress(crypto.SigningKey().VerificationKey().address(), key.EncryptionKey()));
                    }

                    input.attach(crypto.SigningKey().VerificationKey().address());
                } catch (CryptographyError e) {
                    Assert.fail();
                }

                ShuffleMachine machine = standardTestInitialization(τ, crypto);

                try {
                    machine.decryptAll(new MockMessage().attach(input), key, i);
                    Assert.fail("Exception should have been thrown.");
                } catch (FormatException | CryptographyError e) {
                }
            }
        } catch (InvalidImplementationError e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testReadNewAddresses() {
        MockCrypto crypto = new MockCrypto(84512);

        // Success cases.
        try {
            for (int i = 0; i <= 5; i++) {
                Message expected = new MockMessage();
                Message input = new MockMessage();

                for (int j = 0; j <= i; j ++) {
                    Address addr = crypto.SigningKey().VerificationKey().address();

                    expected.attach(addr);
                    input.attach(addr);
                }

                SessionIdentifier τ = new MockSessionIdentifier();
                ShuffleMachine machine = standardTestInitialization(τ, crypto);

                Queue<Address> result = machine.readNewAddresses(new MockMessage().attach(input));

                Assert.assertTrue(expected.equals(new MockMessage().attachAddrs(result)));
            }
        } catch (CryptographyError | FormatException | InvalidImplementationError e) {
            Assert.fail();
        }

        // fail cases.
        try {
            for (int i = 0; i <= 5; i++) {
                Message input = new MockMessage();

                for (int j = 0; j <= i; j++) {
                    input.attach(crypto.SigningKey().VerificationKey().address());
                }

                input.attach(new MockEncryptionKey(14));
                SessionIdentifier τ = new MockSessionIdentifier();
                ShuffleMachine machine = standardTestInitialization(τ, crypto);

                try {
                    machine.readNewAddresses(new MockMessage().attach(input));
                    Assert.fail();
                } catch (FormatException e) {
                }
            }
        } catch (CryptographyError | InvalidImplementationError e) {
            Assert.fail();
        }
    }
}
