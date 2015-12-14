package com.shuffle.form;

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

    private ShuffleMachine shuffleTestInitialization(VerificationKey key, int rand[]) {
        return new ShuffleMachine(
                new MockPacketFactory(key),
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
            MockCrypto crypto = new MockCrypto();
            VerificationKey key = null;
            try {
                key = crypto.SigningKey().VerificationKey();
            } catch (CryptographyException e) {}
            ShuffleMachine machine = shuffleTestInitialization(key, test.randomSequence);

            MockPacket input = new MockPacket(test.input, key);
            MockPacket expected = new MockPacket(test.expected, key);
            try {
                Packet result = machine.shuffle(input);
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

    Map<VerificationKey, Packet> mockPacketMap(int[] input) {
        Map<VerificationKey, Packet> map = new HashMap<>();

        int index = 0;
        for(int i : input) {
            MockVerificationKey key = new MockVerificationKey(index);
            map.put(key, new MockPacket(i, key));
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
            }
        }
    }

    private ShuffleMachine standardTestInitialization(VerificationKey key, Crypto crypto) {
        return new ShuffleMachine(
                new MockPacketFactory(key),
                crypto,
                new MockCoin(),
                new MockNetwork());
    }

    @Test
    public void testDecryptAll() {
        MockCrypto crypto = new MockCrypto();

        // Success cases.
        try {
            for(int i = 0; i <= 5; i++) {
                Queue<MockPacket.Atom> input = new LinkedList<>();
                Queue<MockPacket.Atom> output = new LinkedList<>();
                DecryptionKey key = crypto.DecryptionKey();

                for (int j = 0; j <= i; j ++) {
                    MockPacket.Atom atom = new MockPacket.Atom(crypto.SigningKey().VerificationKey());

                    output.add(atom);
                    input.add(new MockPacket.Atom(new MockPacket.Encrypted(key.EncryptionKey(), atom)));
                }

                ShuffleMachine machine = standardTestInitialization(crypto.SigningKey().VerificationKey(), crypto);

                VerificationKey vk = crypto.SigningKey().VerificationKey();
                Packet result = machine.decryptAll(key, new MockPacket(input, vk));

                Assert.assertTrue(result.equals(new MockPacket(output, vk)));
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
                Queue<MockPacket.Atom> input = new LinkedList<>();
                DecryptionKey key = crypto.DecryptionKey();

                for (int j = 0; j <= i; j++) {
                    MockPacket.Atom atom = new MockPacket.Atom(crypto.SigningKey().VerificationKey());

                    input.add(new MockPacket.Atom(new MockPacket.Encrypted(key.EncryptionKey(), atom)));
                }

                ShuffleMachine machine = standardTestInitialization(crypto.SigningKey().VerificationKey(), crypto);

                VerificationKey vk = crypto.SigningKey().VerificationKey();
                try {
                machine.decryptAll(key, new MockPacket(input, vk));
                } catch (FormatException e) {
                }
            }
        } catch (CryptographyException | InvalidImplementationException e) {
            Assert.fail();
        }
    }

    @Test
    public void testReadNewAddresses() {
        MockCrypto crypto = new MockCrypto();

        // Success cases.
        try {
            for (int i = 0; i <= 5; i++) {
                Queue<VerificationKey> expected = new LinkedList<>();
                Queue<MockPacket.Atom> input = new LinkedList<>();

                for (int j = 0; j <= i; j ++) {
                    VerificationKey key = crypto.SigningKey().VerificationKey();

                    expected.add(key);
                    input.add(new MockPacket.Atom(key));
                }

                ShuffleMachine machine = standardTestInitialization(crypto.SigningKey().VerificationKey(), crypto);

                Queue<VerificationKey> result = machine.readNewAddresses(new MockPacket(input, crypto.SigningKey().VerificationKey()));

                Assert.assertTrue(result.equals(expected));
            }
        } catch (CryptographyException | FormatException | InvalidImplementationException e) {
            Assert.fail();
        }

        // fail cases.
        try {
            for (int i = 0; i <= 5; i++) {
                Queue<MockPacket.Atom> input = new LinkedList<>();

                for (int j = 0; j <= i; j++) {
                    VerificationKey key = crypto.SigningKey().VerificationKey();

                    input.add(new MockPacket.Atom(key));
                }

                input.add(new MockPacket.Atom(3));
                ShuffleMachine machine = standardTestInitialization(crypto.SigningKey().VerificationKey(), crypto);

                try {
                    machine.readNewAddresses(new MockPacket(input, crypto.SigningKey().VerificationKey()));
                    Assert.fail();
                } catch (FormatException e) {
                }
            }
        } catch (CryptographyException | InvalidImplementationException e) {
            Assert.fail();
        }
    }
}
