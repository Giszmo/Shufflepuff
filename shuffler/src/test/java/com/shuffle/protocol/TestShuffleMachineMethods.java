/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.mock.InsecureRandom;
import com.shuffle.mock.MockAddress;
import com.shuffle.mock.MockCoin;
import com.shuffle.mock.MockCrypto;
import com.shuffle.mock.MockEncryptedAddress;
import com.shuffle.mock.MockEncryptionKey;
import com.shuffle.mock.MockMessage;
import com.shuffle.mock.MockMessageFactory;
import com.shuffle.mock.MockNetwork;
import com.shuffle.mock.MockSessionIdentifier;
import com.shuffle.mock.MockSigningKey;
import com.shuffle.mock.MockVerificationKey;
import com.shuffle.mock.RandomSequence;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Tests the methods in the shuffle machine other than the main ones.
 *
 * Created by Daniel Krawisz on 12/5/15.
 */
public class TestShuffleMachineMethods {
    private static final Logger log = LogManager.getLogger(TestShuffleMachineMethods.class);

    private CoinShuffle.Round net(
            int seed,
            MockSessionIdentifier session,
            MockSigningKey sk,
            Map<Integer, VerificationKey> players,
            Phase phase,
            MockMessageFactory messages,
            Network network) throws InvalidParticipantSetException {
        long amount = 20L;

        SortedSet<VerificationKey> playerSet = new TreeSet<>();
        playerSet.addAll(players.values());
        CoinShuffle shuffle = new CoinShuffle(
                messages, new MockCrypto(new InsecureRandom(seed)), new MockCoin()
        );
        Machine machine = new Machine(session, amount, sk, playerSet);
        machine.phase = phase;
        return shuffle.new Round(
                machine, players, null, new Mailbox(session, sk, playerSet, network)
        );
    }

    static class PlayerSetTestCase {
        final int i; // Minimum number blockchain take.
        final int n; // Maximum number blockchain take.
        final int N; // Number of players.
        final int player; // Which player is us.
        final int[] expected; // Which keys should have been returned.

        PlayerSetTestCase(int i, int n, int N, int player, int[] expected) {
            this.i = i;
            this.n = n;
            this.N = N;
            this.player = player;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return "player set test case {" + i + ", " + n + ", "
                    + N + ", " + player + ", " + Arrays.toString(expected) + "}";
        }
    }

    @Test
    public void testPlayerSet() throws InvalidParticipantSetException {
        PlayerSetTestCase[] tests =
                new PlayerSetTestCase[]{
                        new PlayerSetTestCase(
                                1,5,1,1,
                                new int[]{1}
                        ),
                        new PlayerSetTestCase(
                                1,5,5,1,
                                new int[]{1,2,3,4,5}
                        ),
                        new PlayerSetTestCase(
                                1,3,5,1,
                                new int[]{1,2,3}
                        ),
                        new PlayerSetTestCase(
                                2,4,5,3,
                                new int[]{2,3,4}
                        ),
                        new PlayerSetTestCase(
                                -1,7,5,3,
                                new int[]{1,2,3,4,5}
                        ),
                        new PlayerSetTestCase(
                                4,7,5,1,
                                new int[]{4,5}
                        )
                };

        int i = 0;
        for (PlayerSetTestCase test : tests) {
            // make the set of players.
            TreeMap<Integer, VerificationKey> players = new TreeMap<>();
            for (int j = 1; j <= test.N; j ++) {
                players.put(j, new MockVerificationKey(j));
            }

            Set<VerificationKey> result = null;
            // Set up the network operations object.
            CoinShuffle.Round netop = net(
                    234,
                    new MockSessionIdentifier("testPlayerSet" + i),
                    new MockSigningKey(test.player),
                    players,
                    Phase.Shuffling,
                    new MockMessageFactory(),
                    new MockNetwork()
            );
            result = netop.playerSet(test.i, test.n);

            for (int expect : test.expected) {
                Assert.assertTrue(String.format(
                        "Unable to remove expected player %d",expect),result.remove(
                            new MockVerificationKey(expect)));
            }

            Assert.assertTrue("Not every expected player was removed.",result.isEmpty());
            i++;
        }
    }

    private class ShuffleTestCase {
        final int[] randomSequence;
        final int[] input;
        final int[] expected;

        ShuffleTestCase(int[] randomSequence, int[] input, int[] expected) {
            this.randomSequence = randomSequence;
            this.input = input;
            this.expected = expected;
        }
    }

    private CoinShuffle shuffleTestInitialization(int[] rand) {
        return new CoinShuffle(
                new MockMessageFactory(),
                new MockCrypto(
                        new RandomSequence(rand)),
                new MockCoin());
    }

    @Test
    // This tests some cases for the function that randomly shuffles a message.
    public void shuffleTest() {
        ShuffleTestCase[] tests = new ShuffleTestCase[]{
                // The empty case, of course!
                new ShuffleTestCase(
                        new int[]{},
                        new int[]{},
                        new int[]{}
                ),
                // One element.
                new ShuffleTestCase(
                        new int[]{0},
                        new int[]{1},
                        new int[]{1}
                ),
                // All possibilities for two elements.
                new ShuffleTestCase(
                        new int[]{1, 0},
                        new int[]{1, 2},
                        new int[]{2, 1}
                ),
                new ShuffleTestCase(
                        new int[]{0, 0},
                        new int[]{1, 2},
                        new int[]{1, 2}
                ),
                // Most possibilities for three elements.
                new ShuffleTestCase(
                        new int[]{2, 0, 0},
                        new int[]{1, 2, 3},
                        new int[]{3, 1, 2}
                ),
                new ShuffleTestCase(
                        new int[]{2, 1, 0},
                        new int[]{1, 2, 3},
                        new int[]{3, 2, 1}
                ),
                new ShuffleTestCase(
                        new int[]{1, 1, 0},
                        new int[]{1, 2, 3},
                        new int[]{2, 1, 3}
                ),
                new ShuffleTestCase(
                        new int[]{0, 1, 0},
                        new int[]{1, 2, 3},
                        new int[]{1, 3, 2}
                ),
                // Some randomly generated larger cases.
                new ShuffleTestCase(
                        new int[]{1, 0, 1, 0},
                        new int[]{1, 2, 3, 4},
                        new int[]{2, 3, 1, 4}
                ),
                new ShuffleTestCase(
                        new int[]{2, 3, 1, 0, 0},
                        new int[]{1, 2, 3, 4, 5},
                        new int[]{3, 2, 5, 1, 4}
                ),
                new ShuffleTestCase(
                        new int[]{3, 2, 0, 0, 1, 0},
                        new int[]{1, 2, 3, 4, 5, 6},
                        new int[]{4, 1, 2, 3, 6, 5}
                ),
        };

        for (ShuffleTestCase test : tests) {
            CoinShuffle machine = shuffleTestInitialization(test.randomSequence);

            Message input = new MockMessage();
            for (int i : test.input) {
                input = input.attach(new MockAddress(i));
            }
            Message expected = new MockMessage();
            for (int i : test.expected) {
                expected = expected.attach(new MockAddress(i));
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

    private class AreEqualTestCase {
        final List<Message> input = new LinkedList<>();
        final boolean expected;

        AreEqualTestCase(int[] input, boolean expected) {

            for (int i : input) {
                this.input.add(new MockMessage().attach(new MockAddress(i)));
            }

            this.expected = expected;
        }

        AreEqualTestCase(int[][] input, boolean expected) {

            for (int[] in : input) {
                Message queue = new MockMessage();
                for (int i : in) {
                    queue = queue.attach(new MockAddress(i));
                }

                this.input.add(new MockMessage().attach(new MockMessage.Hash(queue)));
            }

            this.expected = expected;
        }
    }

    @Test
    public void testAreEqual() {
        AreEqualTestCase[] tests = new AreEqualTestCase[]{
                // The empty case, of course!
                new AreEqualTestCase(
                        new int[]{},
                        true
                ),
                new AreEqualTestCase(
                        new int[]{1},
                        true
                ),
                new AreEqualTestCase(
                        new int[]{1, 1},
                        true
                ),
                new AreEqualTestCase(
                        new int[]{1, 2},
                        false
                ),
                new AreEqualTestCase(
                        new int[]{2, 2, 2},
                        true
                ),
                new AreEqualTestCase(
                        new int[]{2, 3, 2},
                        false
                ),
                new AreEqualTestCase(
                        new int[]{3, 2, 2},
                        false
                ),
                new AreEqualTestCase(
                        new int[]{2, 2, 3},
                        false
                ),
                new AreEqualTestCase(
                        new int[][]{new int[]{2, 3}, new int[]{2, 3}, new int[]{1, 3}},
                        false
                )
        };

        int i = 0;
        for (AreEqualTestCase testCase : tests) {
            i++;
            try {
                System.out.println("test case... input " + testCase.input.toString());
                Assert.assertEquals("Failure in test case " + i,
                        testCase.expected, CoinShuffle.areEqual(testCase.input));
            } catch (InvalidImplementationError e) {
                Assert.fail("Tests have failed due blockchain error in test class.");
            } catch (CryptographyError e) {
                Assert.fail("Unexpected CryptographyExcption.");
            }
        }
    }

    private CoinShuffle.Round standardTestInitialization(
            SessionIdentifier session,
            SigningKey sk,
            SortedSet<VerificationKey> playerSet,
            Crypto crypto, Mailbox mailbox) throws InvalidParticipantSetException {

        int i = 1;
        Map<Integer, VerificationKey> players = new HashMap<>();
        for (VerificationKey key : playerSet) {
            players.put(i, key);
            i++;
        }

        long amount = 20L;

        CoinShuffle shuffle = new CoinShuffle(new MockMessageFactory(), crypto, new MockCoin());

        return shuffle.new Round(new Machine(
                session, amount, sk, playerSet), players, null, mailbox);
    }

    @Test
    public void testDecryptAll() throws InterruptedException {
        MockCrypto crypto = new MockCrypto(new InsecureRandom(56));
        MessageFactory messages = new MockMessageFactory();

        // Success cases.
        try {
            for (int i = 0; i <= 5; i++) {
                Message input = messages.make();
                Message output = messages.make();
                DecryptionKey dk = crypto.makeDecryptionKey();
                SortedSet<VerificationKey> playerSet = new TreeSet<>();

                for (int j = 0; j <= i; j ++) {
                    VerificationKey key = new MockSigningKey(j + 1).VerificationKey();
                    playerSet.add(key);
                    Address addr = key.address();

                    output = output.attach(addr);
                    input = input.attach(new MockEncryptedAddress(addr, dk.EncryptionKey()));
                }

                SessionIdentifier mockSessionIdentifier
                        = new MockSessionIdentifier("testDecryptAll" + i);
                SigningKey sk = new MockSigningKey(1);
                Mailbox mailbox
                        = new Mailbox(mockSessionIdentifier, sk, playerSet, new MockNetwork());

                CoinShuffle.Round round = standardTestInitialization(
                        mockSessionIdentifier, sk, playerSet, crypto, mailbox
                );

                Message result = round.decryptAll(input, dk, i + 1);

                Assert.assertNotNull(result);
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
    }

    @Test
    public void testReadNewAddresses() {
        MockCrypto crypto = new MockCrypto(new InsecureRandom(84512));

        // Success cases.
        try {
            for (int i = 0; i <= 5; i++) {
                Message expected = new MockMessage();
                Message input = new MockMessage();
                SortedSet<VerificationKey> playerSet = new TreeSet<>();

                for (int j = 0; j <= i; j ++) {
                    VerificationKey vk = new MockSigningKey(j + 1).VerificationKey();
                    playerSet.add(vk);
                    Address addr = vk.address();

                    expected = expected.attach(addr);
                    input = input.attach(addr);
                }

                SessionIdentifier mockSessionIdentifier
                        = new MockSessionIdentifier("testReadNewAddresses" + i);
                SigningKey sk = new MockSigningKey(1);
                Mailbox mailbox
                        = new Mailbox(mockSessionIdentifier, sk, playerSet, new MockNetwork());
                CoinShuffle.Round round = standardTestInitialization(
                                mockSessionIdentifier, sk, playerSet, crypto, mailbox
                );

                Deque<Address> result = round.readNewAddresses(input);

                Assert.assertTrue(expected.equals(new MockMessage().attachAddrs(result)));
            }
        } catch (CryptographyError | FormatException | InvalidImplementationError
                | InvalidParticipantSetException e) {
            e.printStackTrace();
            Assert.fail();
        }

        // fail cases.
        try {
            for (int i = 0; i <= 5; i++) {
                Message input = new MockMessage();
                SortedSet<VerificationKey> playerSet = new TreeSet<>();

                for (int j = 0; j <= i; j++) {
                    VerificationKey vk = new MockSigningKey(j + 1).VerificationKey();
                    playerSet.add(vk);
                    input = input.attach(vk.address());
                }

                input = input.attach(new MockEncryptionKey(14));
                SessionIdentifier mockSessionIdentifier
                        = new MockSessionIdentifier("testReadNewAddressesfail" + i);
                SigningKey sk = new MockSigningKey(1);
                Mailbox mailbox = new Mailbox(
                        mockSessionIdentifier, sk, playerSet, new MockNetwork()
                );

                CoinShuffle.Round round = standardTestInitialization(
                        mockSessionIdentifier, sk, playerSet, crypto, mailbox
                );

                try {
                    round.readNewAddresses(input);
                    Assert.fail();
                } catch (FormatException ignored) {
                }
            }
        } catch (CryptographyError | InvalidParticipantSetException
                | InvalidImplementationError e) {
            Assert.fail();
        }
    }
}
