package com.shuffle.protocol;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Tests the methods in the shuffle machine other than the main ones.
 *
 * Created by Daniel Krawisz on 12/5/15.
 */
public class TestShuffleMachineMethods {
    private static Logger log= LogManager.getLogger(TestShuffleMachineMethods.class);
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

    private CoinShuffle shuffleTestInitialization(int rand[]) {
        return new CoinShuffle(
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
            CoinShuffle machine = shuffleTestInitialization(test.randomSequence);

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
                log.info("got " + result.toString() + "; expected " + expected.toString());
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
        List<Message> input = new LinkedList<>();;
        boolean expected;

        areEqualTestCase(int[] input, boolean expected) {

            for(int i : input) {
                this.input.add(new MockMessage().attach(new MockAddress(i)));
            }

            this.expected = expected;
        }

        areEqualTestCase(int[][] input, boolean expected) {

            for(int[] in : input) {
                Queue<MockMessage.Atom> queue = new LinkedList<>();
                for (int i : in) {
                    queue.add(new MockMessage.Atom(new MockAddress(i)));
                }

                this.input.add(new MockMessage().attach(new MockMessage.Hash(queue)));
            }

            this.expected = expected;
        }
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
                ),
                new areEqualTestCase(
                        new int[][]{new int[]{2, 3}, new int[]{2, 3}, new int[]{1, 3}},
                        false
                )
        };

        for(areEqualTestCase testCase : tests) {
            try {
                Assert.assertEquals(testCase.expected, CoinShuffle.areEqual(testCase.input));
            } catch (InvalidImplementationError e) {
                Assert.fail("Tests have failed due blockchain error in test class.");
            } catch (CryptographyError e) {
                Assert.fail("Unexpected CryptographyExcption.");
            }
        }
    }

    private CoinShuffle.ShuffleMachine standardTestInitialization(
            SessionIdentifier session, SigningKey sk, SortedSet<VerificationKey> players, Crypto crypto) {
        return new CoinShuffle(
                new MockMessageFactory(),
                crypto,
                new MockCoin(),
                new MockNetwork()).new ShuffleMachine(
                    session, 20l, sk, players, null, 0, 2);
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
                DecryptionKey dk = crypto.makeDecryptionKey();
                SortedSet<VerificationKey> playerSet = new TreeSet<>();
                Map<Integer, VerificationKey> players = new HashMap<>();

                for (int j = 0; j <= i; j ++) {
                    VerificationKey key = new MockSigningKey(j + 1).VerificationKey();
                    playerSet.add(key);
                    players.put(j + 1, key);
                    Address addr = key.address();

                    output.attach(addr);
                    input.attach(new MockEncryptedAddress(addr, dk.EncryptionKey()));
                }

                SessionIdentifier mockSessionIdentifier = new MockSessionIdentifier("testDecryptAll" + i);
                SigningKey sk = new MockSigningKey(1);
                CoinShuffle.ShuffleMachine.Round round =
                        standardTestInitialization(mockSessionIdentifier, sk, playerSet, crypto).new Round(players, null);

                Message result = round.decryptAll(new MockMessage().attach(input), dk, i + 1);

                Assert.assertTrue(result.equals(output));
            }
        } catch (CryptographyError e) {
            Assert.fail("Unexpected CryptographyError:");
        } catch (FormatException e) {
            e.printStackTrace();
            Assert.fail("Unexpected FormatException:");
        } catch (InvalidImplementationError e) {
            Assert.fail("Unexpected InvalidImplementationError:");
        } catch (InvalidParticipantSetException e) {
            Assert.fail("Unexpected InvalidParticipantSetException");
        }

        // Fail cases.
        // TODO: include fail cases that lead blockchain blame cases.
        try {
            for(int i = 0; i <= 5; i++) {
                MockSessionIdentifier mockSessionIdentifier = new MockSessionIdentifier("testDecryptAllfail" + i);
                Message input = new MockMessage();
                DecryptionKey dk = null;
                SortedSet<VerificationKey> playerSet = new TreeSet<>();
                Map<Integer, VerificationKey> players = new HashMap<>();
                try {
                    dk = crypto.makeDecryptionKey();

                    for (int j = 0; j <= i; j++) {
                        VerificationKey vk = new MockSigningKey(j + 1).VerificationKey();
                        playerSet.add(vk);
                        players.put(j + 1, vk);

                        input.attach(new MockEncryptedAddress(vk.address(), dk.EncryptionKey()));
                    }

                    input.attach(crypto.makeSigningKey().VerificationKey().address());
                } catch (CryptographyError e) {
                    Assert.fail();
                }

                SigningKey sk = new MockSigningKey(1);
                CoinShuffle.ShuffleMachine.Round round =
                        standardTestInitialization(mockSessionIdentifier, sk, playerSet, crypto).new Round(players, null);

                try {
                    round.decryptAll(new MockMessage().attach(input), dk, i);
                    Assert.fail("Exception should have been thrown.");
                } catch (FormatException | CryptographyError e) {
                }
            }
        } catch (InvalidImplementationError e) {
            e.printStackTrace();
            Assert.fail();
        } catch (InvalidParticipantSetException e) {
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
                SortedSet<VerificationKey> playerSet = new TreeSet<>();
                Map<Integer, VerificationKey> players = new HashMap<>();

                for (int j = 0; j <= i; j ++) {
                    VerificationKey vk = new MockSigningKey(j + 1).VerificationKey();
                    playerSet.add(vk);
                    players.put(j + 1, vk);
                    Address addr = vk.address();

                    expected.attach(addr);
                    input.attach(addr);
                }

                SessionIdentifier mockSessionIdentifier = new MockSessionIdentifier("testReadNewAddresses" + i);
                SigningKey sk = new MockSigningKey(1);
                CoinShuffle.ShuffleMachine.Round round =
                        standardTestInitialization(mockSessionIdentifier, sk, playerSet, crypto).new Round(players, null);

                Queue<Address> result = round.readNewAddresses(new MockMessage().attach(input));

                Assert.assertTrue(expected.equals(new MockMessage().attachAddrs(result)));
            }
        } catch (CryptographyError | FormatException | InvalidImplementationError | InvalidParticipantSetException e) {
            e.printStackTrace();
            Assert.fail();
        }

        // fail cases.
        try {
            for (int i = 0; i <= 5; i++) {
                Message input = new MockMessage();
                SortedSet<VerificationKey> playerSet = new TreeSet<>();
                Map<Integer, VerificationKey> players = new HashMap<>();

                for (int j = 0; j <= i; j++) {
                    VerificationKey vk = new MockSigningKey(j + 1).VerificationKey();
                    playerSet.add(vk);
                    players.put(j + 1, vk);
                    input.attach(vk.address());
                }

                input.attach(new MockEncryptionKey(14));
                SessionIdentifier mockSessionIdentifier = new MockSessionIdentifier("testReadNewAddressesfail" + i);
                SigningKey sk = new MockSigningKey(1);
                CoinShuffle.ShuffleMachine.Round round =
                        standardTestInitialization(mockSessionIdentifier, sk, playerSet, crypto).new Round(players, null);

                try {
                    round.readNewAddresses(new MockMessage().attach(input));
                    Assert.fail();
                } catch (FormatException e) {
                }
            }
        } catch (CryptographyError | InvalidParticipantSetException | InvalidImplementationError e) {
            Assert.fail();
        }
    }
}
