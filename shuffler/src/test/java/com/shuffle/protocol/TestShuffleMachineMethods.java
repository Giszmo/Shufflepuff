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
import java.util.SortedSet;
import java.util.TreeSet;

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
            MockCrypto crypto = new MockCrypto(90);
            SigningKey key = null;
            try {
                key = crypto.makeSigningKey();
            } catch (CryptographyError e) {}
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
                Assert.assertEquals(testCase.expected, CoinShuffle.areEqual(mockPacketMap(testCase.input).values()));
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
                DecryptionKey dk = crypto.DecryptionKey();
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

                SessionIdentifier mockSessionIdentifier = new MockSessionIdentifier();
                SigningKey sk = new MockSigningKey(1);
                CoinShuffle.ShuffleMachine.Round round =
                        standardTestInitialization(mockSessionIdentifier, sk, playerSet, crypto).new Round(players, null);


                Message result = round.decryptAll(new MockMessage().attach(input), dk, i);

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
                MockSessionIdentifier mockSessionIdentifier = new MockSessionIdentifier();
                Message input = new MockMessage();
                DecryptionKey dk = null;
                SortedSet<VerificationKey> playerSet = new TreeSet<>();
                Map<Integer, VerificationKey> players = new HashMap<>();
                try {
                    dk = crypto.DecryptionKey();

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

                SessionIdentifier mockSessionIdentifier = new MockSessionIdentifier();
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
                SessionIdentifier mockSessionIdentifier = new MockSessionIdentifier();
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
