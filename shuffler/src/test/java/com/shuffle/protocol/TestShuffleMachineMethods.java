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
import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.mock.InsecureRandom;
import com.shuffle.mock.MockAddress;
import com.shuffle.mock.MockCoin;
import com.shuffle.mock.MockCrypto;
import com.shuffle.mock.MockEncryptedAddress;
import com.shuffle.mock.MockEncryptionKey;
import com.shuffle.player.Messages;
import com.shuffle.mock.MockSessionIdentifier;
import com.shuffle.mock.MockSigningKey;
import com.shuffle.mock.MockVerificationKey;
import com.shuffle.mock.RandomSequence;
import com.shuffle.chan.packet.SessionIdentifier;
import com.shuffle.protocol.message.Message;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
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
            MockSigningKey sk,
            Address addr,
            Map<Integer, VerificationKey> players,
            Messages messages) throws InvalidParticipantSetException {

        long amount = 20L;

        SortedSet<VerificationKey> playerSet = new TreeSet<>();
        playerSet.addAll(players.values());
        CoinShuffle shuffle = new CoinShuffle(
                messages, new MockCrypto(new InsecureRandom(seed)), new MockCoin()
        );
        CoinShuffle.CurrentPhase machine = new CoinShuffle.CurrentPhase();
        return shuffle.new Round(
                machine, amount, sk, players, addr, null, new Mailbox(sk.VerificationKey(), playerSet, messages)
        );
    }

    static class PlayerSetTestCase {
        final int i; // Minimum number blockchain take.
        final int n; // Maximum number blockchain take.
        final int N; // Number of players.
        final int player; // Which player is us.
        final int[] expected; // Which keys should have been returned.
        final Set<SigningKey> players;

        PlayerSetTestCase(int i, int n, int N, int player, int[] expected) {
            this.i = i;
            this.n = n;
            this.N = N;
            this.player = player;
            this.expected = expected;

            players = new TreeSet<>();
            for (int j = 0; j < N; j ++) {
                players.add(new MockSigningKey(j + 1));
            }
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

            MockSessionIdentifier session = new MockSessionIdentifier("testPlayerSet" + i);
            MockSigningKey me = new MockSigningKey(test.player);

            MockNetwork network = new MockNetwork(session, me, test.players, 100);

            // Set up the network operations object.
            CoinShuffle.Round netop = net(
                    234, me, new MockAddress(-1), players,
                    network.messages(me.VerificationKey()));

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

    private CoinShuffle shuffleTestInitialization(Messages messages, int[] rand) {
        return new CoinShuffle(
                messages,
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

        SigningKey me = new MockSigningKey(1);
        Set<SigningKey> players = new TreeSet<>();
        players.add(me);
        players.add(new MockSigningKey(2));
        players.add(new MockSigningKey(3));

        for (ShuffleTestCase test : tests) {
            SessionIdentifier session = new MockSessionIdentifier("shuffle test");
            MockNetwork network = new MockNetwork(session, me, players, 100);

            Messages messages = network.messages(me.VerificationKey());

            CoinShuffle machine = shuffleTestInitialization(messages, test.randomSequence);

            Message input = messages.make();
            for (int i : test.input) {
                input = input.attach(new MockAddress(i));
            }
            Message expected = messages.make();
            for (int i : test.expected) {
                expected = expected.attach(new MockAddress(i));
            }
            try {
                Message result = machine.shuffle(input);
                log.info("got " + result.toString() + "; expected " + expected.toString());
                Assert.assertTrue(result.equals(expected));
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
        final SigningKey sk = new MockSigningKey(1);
        final Messages messages;

        AreEqualTestCase(int[] input, boolean expected) {
            final Set<SigningKey> players = new TreeSet<>();
            players.add(sk);
            players.add(new MockSigningKey(2));

            messages = new MockNetwork(
                    new MockSessionIdentifier("test are equal"),
                    sk, players, 100).messages(sk.VerificationKey());

            for (int i : input) {
                this.input.add(messages.make().attach(new MockAddress(i)));
            }

            this.expected = expected;
        }

        AreEqualTestCase(int[][] input, boolean expected) {
            final Set<SigningKey> players = new TreeSet<>();
            players.add(sk);
            players.add(new MockSigningKey(2));

            messages = new MockNetwork(
                    new MockSessionIdentifier("test are equal"),
                    sk, players, 100).messages(sk.VerificationKey());

            for (int[] in : input) {
                Message queue = messages.make();
                for (int i : in) {
                    queue = queue.attach(new MockAddress(i));
                }

                this.input.add(queue.hashed());
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
            }
        }
    }

    private CoinShuffle.Round standardTestInitialization(
            SessionIdentifier session,
            int me,
            Address addr,
            SortedSet<SigningKey> others,
            Crypto crypto, Mailbox mailbox) throws InvalidParticipantSetException {

        int i = 1;
        Map<Integer, VerificationKey> players = new HashMap<>();
        for (SigningKey key : others) {
            players.put(i, key.VerificationKey());
            i++;
        }

        SigningKey sk = new MockSigningKey(me);

        long amount = 20L;

        MockNetwork net = new MockNetwork(session, sk, others, 100);

        CoinShuffle shuffle = new CoinShuffle(net.messages(sk.VerificationKey()),
                crypto, new MockCoin());

        return shuffle.new Round(
                new CoinShuffle.CurrentPhase(), amount, sk, players, addr, null, mailbox
        );
    }

    @Test
    public void testDecryptAll() throws WaitingException, InterruptedException, IOException {

        MockCrypto crypto = new MockCrypto(new InsecureRandom(56));

        // Success cases.
        try {
            for (int i = 0; i <= 5; i++) {

                // Set up a session identifier and signing key.
                SessionIdentifier session
                        = new MockSessionIdentifier("testDecryptAll" + i);

                SortedSet<SigningKey> players = new TreeSet<>();
                for (int j = 0; j <= i; j++) {
                    players.add(new MockSigningKey(j + 1));
                }

                SigningKey sk = new MockSigningKey(1);
                MockNetwork net = new MockNetwork(session, sk, players, 100);

                // Set up the message factory.
                Messages messages = net.messages(sk.VerificationKey());

                // Make the set of messages we want to test.
                Message input = messages.make();
                Message output = messages.make();
                DecryptionKey dk = crypto.makeDecryptionKey();

                SortedSet<VerificationKey> playersPublic = new TreeSet<>();

                for (SigningKey k : players) {
                    VerificationKey key = k.VerificationKey();
                    playersPublic.add(key);
                    Address addr = key.address();

                    output = output.attach(addr);
                    input = input.attach(new MockEncryptedAddress(addr, dk.EncryptionKey()));
                }

                Mailbox mailbox
                        = new Mailbox(sk.VerificationKey(), playersPublic, messages);

                CoinShuffle.Round round = standardTestInitialization(
                        session, 1, new MockAddress(-1), players, crypto, mailbox
                );

                Message result = round.decryptAll(input, dk, i + 1);

                Assert.assertNotNull(result);
                Assert.assertTrue(result.equals(output));
            }
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
        SigningKey sk = new MockSigningKey(1);

        // Success cases.
        try {
            for (int i = 0; i <= 5; i++) {
                SortedSet<SigningKey> players = new TreeSet<>();
                SortedSet<VerificationKey> playerSet = new TreeSet<>();
                for (int j = 0; j <= i; j++) {
                    players.add(new MockSigningKey(j + 1));
                    VerificationKey vk = new MockSigningKey(j + 1).VerificationKey();
                    playerSet.add(vk);
                }

                SessionIdentifier mockSessionIdentifier
                        = new MockSessionIdentifier("testReadNewAddresses" + i);
                MockNetwork net = new MockNetwork(
                        mockSessionIdentifier, new MockSigningKey(1), players, 100);
                Messages messages = net.messages(sk.VerificationKey());

                Message expected = messages.make();
                Message input = messages.make();

                for (int j = 0; j <= i; j ++) {
                    VerificationKey vk = new MockSigningKey(j + 1).VerificationKey();
                    Address addr = vk.address();

                    expected = expected.attach(addr);
                    input = input.attach(addr);
                }

                Mailbox mailbox
                        = new Mailbox(sk.VerificationKey(), playerSet, messages);
                CoinShuffle.Round round = standardTestInitialization(
                                mockSessionIdentifier, 1, new MockAddress(-1), players, crypto, mailbox
                );

                Deque<Address> newAddrs = round.readNewAddresses(input);
                Message result = messages.make();
                for (Address address : newAddrs) {
                    result = result.attach(address);
                }

                Assert.assertTrue(expected.equals(result));
            }
        } catch (FormatException | InvalidImplementationError
                | InvalidParticipantSetException e) {
            e.printStackTrace();
            Assert.fail();
        }

        // fail cases.
        try {
            for (int i = 0; i <= 5; i++) {
                SortedSet<SigningKey> players = new TreeSet<>();
                SortedSet<VerificationKey> playerSet = new TreeSet<>();
                for (int j = 0; j <= i; j++) {
                    SigningKey k = new MockSigningKey(j + 1);
                    players.add(k);
                    VerificationKey vk = k.VerificationKey();
                    playerSet.add(vk);
                }

                SessionIdentifier mockSessionIdentifier
                        = new MockSessionIdentifier("testReadNewAddressesfail" + i);
                MockNetwork net = new MockNetwork(
                        mockSessionIdentifier, new MockSigningKey(1), players, 100);
                Messages messages = net.messages(sk.VerificationKey());

                Message input = messages.make();

                for (int j = 0; j <= i; j++) {
                    input = input.attach(new MockSigningKey(j + 1).VerificationKey().address());
                }

                input = input.attach(new MockEncryptionKey(14));
                Mailbox mailbox = new Mailbox(sk.VerificationKey(), playerSet, messages);

                CoinShuffle.Round round = standardTestInitialization(
                        mockSessionIdentifier, 1, new MockAddress(-1), players, crypto, mailbox
                );

                try {
                    round.readNewAddresses(input);
                    Assert.fail();
                } catch (FormatException ignored) {
                }
            }
        } catch (InvalidParticipantSetException
                | InvalidImplementationError e) {
            Assert.fail(e.toString());
        }
    }
}
