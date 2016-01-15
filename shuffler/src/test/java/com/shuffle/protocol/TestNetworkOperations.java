package com.shuffle.protocol;

import com.shuffle.cryptocoin.CryptographyError;
import com.shuffle.cryptocoin.VerificationKey;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Tests for certain functions pertaining to network interactions.
 *
 * Created by Daniel Krawisz on 12/7/15.
 */
public class TestNetworkOperations  {

    public CoinShuffle.ShuffleMachine.Round net(
            int seed,
            MockSessionIdentifier session,
            MockSigningKey sk,
            Map<Integer, VerificationKey> players,
            Phase phase,
            MockMessageFactory messages,
            Network network) throws InvalidParticipantSetException {
        SortedSet<VerificationKey> playerSet = new TreeSet<>();
        playerSet.addAll(players.values());
        CoinShuffle.ShuffleMachine machine =
                new CoinShuffle(messages, new MockCrypto(seed), new MockCoin(), network).new
                    ShuffleMachine(session, 20l, sk, playerSet, null, 1, 2);
        machine.phase = phase;
        return machine.new Round(players, null);
    }

    static class playerSetTestCase {
        int i; // Minimum number blockchain take.
        int n; // Maximum number blockchain take.
        int N; // Number of players.
        int player; // Which player is us.
        int[] expected; // Which keys should have been returned.

        playerSetTestCase(int i, int n, int N, int player, int[] expected) {
            this.i = i;
            this.n = n;
            this.N = N;
            this.player = player;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return "player set test case {" + i + ", " + n + ", " + N + ", " + player + ", " + Arrays.toString(expected) + "}";
        }
    }

    @Test
    public void testPlayerSet() {
        playerSetTestCase tests[] =
                new playerSetTestCase[]{
                        new playerSetTestCase(
                                1,5,1,1,
                                new int[]{1}
                        ),
                        new playerSetTestCase(
                                1,5,5,1,
                                new int[]{1,2,3,4,5}
                        ),
                        new playerSetTestCase(
                                1,3,5,1,
                                new int[]{1,2,3}
                        ),
                        new playerSetTestCase(
                                2,4,5,3,
                                new int[]{2,3,4}
                        ),
                        new playerSetTestCase(
                                -1,7,5,3,
                                new int[]{1,2,3,4,5}
                        ),
                        new playerSetTestCase(
                                4,7,5,1,
                                new int[]{4,5}
                        )
                };

        int i = 0;
        for(playerSetTestCase test : tests) {
            // make the set of players.
            TreeMap<Integer, VerificationKey> players = new TreeMap<Integer, VerificationKey>();
            for (int j = 1; j <= test.N; j ++) {
                players.put(j, new MockVerificationKey(j));
            }

            Set<VerificationKey> result = null;
            try {
                // Set up the network operations object.
                CoinShuffle.ShuffleMachine.Round netop =
                    net(234, new MockSessionIdentifier("testPlayerSet" + i), new MockSigningKey(test.player), players,
                        Phase.Shuffling, new MockMessageFactory(), new MockNetwork());
                result = netop.playerSet(test.i, test.n);
            } catch (CryptographyError e) {
                Assert.fail("Unexpected CryptographyException.");
            } catch (InvalidImplementationError e) {
                Assert.fail("Unexpected InvalidImplementationException.");
            } catch (InvalidParticipantSetException e) {
                e.printStackTrace();
                Assert.fail("Unexpected InvalidParticipationSetException");
            } catch (NullPointerException e) {
                Assert.fail();
            }

            for (int expect : test.expected) {
                Assert.assertTrue(result.remove(new MockVerificationKey(expect)));
            }

            Assert.assertTrue(result.isEmpty());
            i++;
        }
    }

    static class broadcastTestCase {
        int recipients;
        int sender;

        public broadcastTestCase(int recipients, int sender) {
            this.recipients = recipients;
            this.sender = sender;
        }
    }

    @Test
    public void testBroadcast() {
        broadcastTestCase tests[] =
                new broadcastTestCase[]{
                        new broadcastTestCase(1, 1),
                        new broadcastTestCase(2, 1),
                        new broadcastTestCase(2, 2),
                        new broadcastTestCase(3, 2),
                        new broadcastTestCase(4, 2),
                };

        for (broadcastTestCase test : tests) {

            // Create mock network object.
            MockNetwork network = new MockNetwork();

            // make the set of players.
            TreeMap<Integer, VerificationKey> players = new TreeMap<Integer, VerificationKey>();
            for (int i = 1; i <= test.recipients; i ++) {
                players.put(i, new MockVerificationKey(i));
            }

            MockMessageFactory messages = new MockMessageFactory();

            // Set up the network operations object.
            try {
                CoinShuffle.ShuffleMachine.Round netop =
                        net(577, new MockSessionIdentifier("broadcastTest" + test.recipients), new MockSigningKey(1), players,
                            Phase.Shuffling, messages, network);
                netop.broadcast(messages.make());
            } catch (TimeoutError e) {
                Assert.fail("Unexpected exception.");
            } catch (CryptographyError e) {
                e.printStackTrace();
                Assert.fail("Unexpected CryptograhyException.");
            } catch (InvalidImplementationError e) {
                Assert.fail("Unexpected InvalidImplementationException.");
            } catch (InvalidParticipantSetException e) {
                e.printStackTrace();
                Assert.fail("Unexpected InvalidParticipantSetException.");
            }
        }
    }

    static class sendToTestCase {
        int sender;
        int recipient;
        int players;
        boolean success;

        public sendToTestCase(int sender, int recipient, int players, boolean success) {
            this.sender = sender;
            this.recipient = recipient;
            this.players = players;
            this.success = success;
        }
    }

    @Test
    public void testSend() {
        sendToTestCase tests[] = new sendToTestCase[]{
                // Case where recipient does not exist.
                new sendToTestCase(1, 2, 2, true),
                // Cannot send to myself.
                new sendToTestCase(1, 1, 2, false)
        };

        int i = 0;
        for (sendToTestCase test : tests) {
            try {
                // The player sending and inbox.
                MockSigningKey sk = new MockSigningKey(1);

                // Create mock network object.
                MockNetwork network = new MockNetwork();

                // make the set of players.
                TreeMap<Integer, VerificationKey> players = new TreeMap<Integer, VerificationKey>();
                for (int j = 1; j <= test.players; j ++) {
                    players.put(j, new MockVerificationKey(j));
                }

                // Set up the shuffle machine (only used blockchain query for the current phase).
                MockMessageFactory message = new MockMessageFactory();
                MockSessionIdentifier mockSessionIdentifier = new MockSessionIdentifier("testSend" + i);

                // Set up the network operations object.
                CoinShuffle.ShuffleMachine.Round netop =
                        net(8989, mockSessionIdentifier, sk, players,
                                Phase.Shuffling, message, network);

                netop.send(new Packet(
                        new MockMessage(), mockSessionIdentifier,
                        Phase.Shuffling,
                        sk.VerificationKey(),
                        new MockVerificationKey(test.recipient)));

                Queue<Map.Entry<Packet, MockVerificationKey>> responses = network.getResponses();
                Assert.assertEquals(1, responses.size());

                for (Map.Entry msg : responses) {
                    Assert.assertTrue(new MockVerificationKey(test.recipient).equals(msg.getValue()));
                }

            } catch (InvalidImplementationError e) {
                Assert.fail("Unexpected InvalidImplementationException");
            } catch (CryptographyError e) {
                Assert.fail("Unexpected CryptographyException");
            } catch (TimeoutError e) {
                Assert.fail("Unexpected TimeoutException");
            } catch (InvalidParticipantSetException e) {
                Assert.fail("Unexpected InvalidParticipationSetException");
            }
            i++;
        }
    }

    static class receiveFromTestCase {
        int[] players;
        int requested; // The player that the message was expected from.
        Phase phase; // The expected phase.
        Packet packet;
        Error e; // If an exception is expected.

        public receiveFromTestCase(int[] players, int requested, Phase phase,Packet packet, Error e) {
            this.players = players;
            this.requested = requested;
            this.packet = packet;
            this.e = e;
            this.phase = phase;
        }
    }

    @Test
    public void testReceiveFrom() {
        receiveFromTestCase tests[] = new receiveFromTestCase[]{
                // time out exception test case.
                new receiveFromTestCase(new int []{1,2,3}, 2, Phase.Shuffling, null, new TimeoutError()),
                // Various malformed inputs.
                // TODO
        };

        int i = 0;
        for (receiveFromTestCase test : tests) {
            try {
                // The player sending and inbox.
                MockSigningKey sk = new MockSigningKey(1);

                // Create mock network object.
                MockNetwork network = new MockNetwork();

                // make the set of players.
                TreeMap<Integer, VerificationKey> players = new TreeMap<Integer, VerificationKey>();
                for (int j = 1; j <= test.players.length; j ++) {
                    players.put(j, new MockVerificationKey(test.players[j - 1]));
                }

                MockSessionIdentifier mockSessionIdentifier = new MockSessionIdentifier("receiveFromTest" + i);

                // Set up the network operations object.
                CoinShuffle.ShuffleMachine.Round netop =
                        net(9341, mockSessionIdentifier, sk, players,
                                Phase.Shuffling, new MockMessageFactory(), network);

                netop.receiveFrom(new MockVerificationKey(test.requested), test.phase);
            } catch (TimeoutError | CryptographyError | FormatException | BlameException
                    | ValueException | InterruptedException | InvalidImplementationError e) {
                if (test.e != null) {
                    if (!test.e.getClass().equals(e.getClass())) {
                        Assert.fail("Wrong exception encountered.");
                    }
                } else {
                    Assert.fail("Unexpected exception encountered.");
                }
            } catch (InvalidParticipantSetException e) {
                Assert.fail();
            }
            i++;
        }
    }

    static class receiveFromMultipleTestCase {
        int[] players;
    }

    @Test
    public void testReceiveFromMultiple() {
        // TODO
        receiveFromMultipleTestCase tests[] = new receiveFromMultipleTestCase[]{};

        int i = 0;
        for (receiveFromMultipleTestCase test : tests) {
            // The player sending and inbox.
            MockSigningKey sk = new MockSigningKey(0);

            // Create mock network object.
            MockNetwork network = new MockNetwork();

            // make the set of players.
            TreeMap<Integer, VerificationKey> players = new TreeMap<Integer, VerificationKey>();
            for (int j = 1; j <= test.players.length; j ++) {
                players.put(j, new MockVerificationKey(test.players[j]));
            }

            MockSessionIdentifier mockSessionIdentifier = new MockSessionIdentifier("receiveFromMultiple" + i);

            // Set up the network operations object.
            try {
                CoinShuffle.ShuffleMachine.Round netop =
                        net(475, mockSessionIdentifier, sk, players,
                                Phase.Shuffling, new MockMessageFactory(), network);
            } catch (InvalidParticipantSetException e) {
                Assert.fail();
            }
            i++;
        }
    }
}
