package com.shuffle.protocol;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
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
            } catch (CryptographyException e) {}
            SessionIdentifier τ = new MockSessionIdentifier();
            ShuffleMachine machine = shuffleTestInitialization(τ, test.randomSequence);

            ShufflePhase phase = ShufflePhase.Shuffling;

            MockMessage input = new MockMessage(τ, phase, key).attach(test.input);
            MockMessage expected = new MockMessage(τ, phase, key).attach(test.expected);
            try {
                Message result = machine.shuffle(input, key);
                System.out.println("got " + result.toString() + "; expected " + expected.toString());
                Assert.assertTrue(result.equals(expected));
            } catch (CryptographyException e) {
                Assert.fail("Unexpected CryptographyException");
            } catch (InvalidImplementationException e) {
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

    Map<VerificationKey, Message> mockPacketMap(int[] input) throws CryptographyException {
        Map<VerificationKey, Message> map = new HashMap<>();

        int index = 0;
        for(int i : input) {
            MockSigningKey key = new MockSigningKey(index);
            map.put(key.VerificationKey(), new MockMessage(new MockSessionIdentifier(), ShufflePhase.Shuffling, key).attach(i));
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
            } catch (InvalidImplementationException e) {
                Assert.fail("Tests have failed due to error in test class.");
            } catch (CryptographyException e) {
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

        // Success cases.
        try {
            for(int i = 0; i <= 5; i++) {
                Queue<MockMessage.Atom> input = new LinkedList<>();
                Queue<MockMessage.Atom> output = new LinkedList<>();
                DecryptionKey key = crypto.DecryptionKey();

                for (int j = 0; j <= i; j ++) {
                    MockMessage.Atom atom = new MockMessage.Atom(crypto.SigningKey().VerificationKey());

                    output.add(atom);
                    input.add(new MockMessage.Atom(new MockMessage.Encrypted(key.EncryptionKey(), atom)));
                }

                SessionIdentifier τ = new MockSessionIdentifier();
                ShuffleMachine machine = standardTestInitialization(τ, crypto);

                SigningKey sk = crypto.SigningKey();
                ShufflePhase phase = ShufflePhase.Shuffling;
                Message result = machine.decryptAll(key, new MockMessage(τ, phase, sk).attach(input), sk);

                Assert.assertTrue(result.equals(new MockMessage(τ, phase, sk).attach(output)));
            }
        } catch (CryptographyException e) {
            Assert.fail("Unexpected CryptographyException:");
        } catch (FormatException e) {
            e.printStackTrace();
            Assert.fail("Unexpected FormatException:");
        } catch (InvalidImplementationException e) {
            Assert.fail("Unexpected InvalidImplementationException:");
        }

        // Fail cases.
        try {
            for(int i = 0; i <= 5; i++) {
                MockSessionIdentifier τ = new MockSessionIdentifier();
                Queue<MockMessage.Atom> input = new LinkedList<>();
                DecryptionKey key = crypto.DecryptionKey();

                for (int j = 0; j <= i; j++) {
                    MockMessage.Atom atom = new MockMessage.Atom(crypto.SigningKey().VerificationKey());

                    input.add(new MockMessage.Atom(new MockMessage.Encrypted(key.EncryptionKey(), atom)));
                }

                ShuffleMachine machine = standardTestInitialization(τ, crypto);

                SigningKey sk = crypto.SigningKey();
                try {
                machine.decryptAll(key, new MockMessage(τ, ShufflePhase.Shuffling, sk).attach(input), sk);
                } catch (FormatException e) {
                }
            }
        } catch (CryptographyException | InvalidImplementationException e) {
            Assert.fail();
        }
    }

    @Test
    public void testReadNewAddresses() {
        MockCrypto crypto = new MockCrypto(84512);

        // Success cases.
        try {
            for (int i = 0; i <= 5; i++) {
                Queue<VerificationKey> expected = new LinkedList<>();
                Queue<MockMessage.Atom> input = new LinkedList<>();

                for (int j = 0; j <= i; j ++) {
                    VerificationKey key = crypto.SigningKey().VerificationKey();

                    expected.add(key);
                    input.add(new MockMessage.Atom(key));
                }

                SessionIdentifier τ = new MockSessionIdentifier();
                ShuffleMachine machine = standardTestInitialization(τ, crypto);

                Queue<VerificationKey> result = machine.readNewAddresses(new MockMessage(τ, ShufflePhase.Shuffling, crypto.SigningKey()).attach(input));

                Assert.assertTrue(result.equals(expected));
            }
        } catch (CryptographyException | FormatException | InvalidImplementationException e) {
            Assert.fail();
        }

        // fail cases.
        try {
            for (int i = 0; i <= 5; i++) {
                Queue<MockMessage.Atom> input = new LinkedList<>();

                for (int j = 0; j <= i; j++) {
                    VerificationKey key = crypto.SigningKey().VerificationKey();

                    input.add(new MockMessage.Atom(key));
                }

                input.add(new MockMessage.Atom(3));
                SessionIdentifier τ = new MockSessionIdentifier();
                ShuffleMachine machine = standardTestInitialization(τ, crypto);

                try {
                    machine.readNewAddresses(new MockMessage(τ, ShufflePhase.Shuffling, crypto.SigningKey()).attach(input));
                    Assert.fail();
                } catch (FormatException e) {
                }
            }
        } catch (CryptographyException | InvalidImplementationException e) {
            Assert.fail();
        }
    }
}
