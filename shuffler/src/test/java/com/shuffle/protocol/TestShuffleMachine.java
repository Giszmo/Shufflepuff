package com.shuffle.protocol;

import com.shuffle.cryptocoin.Address;
import com.shuffle.cryptocoin.Coin;
import com.shuffle.cryptocoin.CryptographyError;
import com.shuffle.cryptocoin.SigningKey;
import com.shuffle.cryptocoin.VerificationKey;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Integration tests for the protocol.
 *
 * Created by Daniel Krawisz on 12/10/15.
 */
public class TestShuffleMachine {

    public class Expected {
        SigningKey identity;
        com.shuffle.protocol.Simulator.InitialState input;
        ReturnState output;

        public Expected(SigningKey identity, Simulator.InitialState input, ReturnState output) {
            this.identity = identity;
            this.input = input;
            this.output = output;
        }
    }

    public class testCase {

        String description = null;
        SessionIdentifier τ;
        long amount;
        Coin coin;
        Map<SigningKey, Expected> machines = new HashMap<>();

        testCase(SessionIdentifier τ, long amount, Coin coin, String desc) {
            this.τ = τ;
            this.description = desc;
            this.amount = amount;
            this.coin = coin;
        }

        testCase(SessionIdentifier τ, long amount, Coin coin) {
            this.τ = τ;
            this.amount = amount;
            this.coin = coin;
        }

        testCase put(SigningKey key, Expected ex) {
            machines.put(key, ex);
            return this;
        }
    }

    public class InsufficientFunds {
        final int players; // Number of players.
        final int[] deadbeats; // The players who just don't have enough funds.
        final int[] spenders; // The players who have spent their funds.

        InsufficientFunds(int players, int[] deadbeats, int[] spenders) {
            this.players = players;
            this.deadbeats = deadbeats;
            this.spenders = spenders;
        }

        BlameMatrix getExpectedBlameMatrix() {
            return null;
        }

        Coin getBlockchain() {
            return null;
        }
    }

    public class DoubleSpending {
        final int[] views; // The views of the crypto coin network for different players.
        final int[] doubleSpenders; // The players who spend while the protocol is happening.

        DoubleSpending(int[] views, int[] doubleSpenders) {
            this.views = views;
            this.doubleSpenders = doubleSpenders;
        }

        BlameMatrix getExpectedBlameMatrix() {
            return null;
        }

        Coin getBlockchain() {
            return null;
        }
    }

    @Test
    public void testShuffleMachine() {
        Map<Integer, testCase> tests = new HashMap<>();

        // Tests for successful runs.
        int caseNo = 0;
        for (int players = 2; players <= 12; players ++ ) {
            try {

                SessionIdentifier τ = new MockSessionIdentifier();
                MockCoin coin = new MockCoin();
                testCase test = new testCase(τ, 17, coin, "successful run with " + players + " players.");

                for (int i = 1; i <= players; i++) {
                    MockSigningKey key = new MockSigningKey(i);
                    Address address = key.VerificationKey().address();
                    coin.put(address, 20);
                    test.put(key,
                            new Expected(key,
                                    new Simulator.InitialState(key),
                                    new ReturnState(true, τ, ShufflePhase.Completed, null, null)
                            ));
                }
                tests.put(caseNo, test);
                caseNo ++;
            } catch (CryptographyError e) {
                Assert.fail("could not create test case " + caseNo);
            }
        }

        // Tests for players who initially have insufficient funds.
        InsufficientFunds[] insufficientFundsCases = new InsufficientFunds[]{
                new InsufficientFunds(10, new int[]{3}, new int[]{}),
                new InsufficientFunds(10, new int[]{}, new int[]{4}),
                new InsufficientFunds(10, new int[]{5, 10}, new int[]{}),
                new InsufficientFunds(10, new int[]{2}, new int[]{8}),
                new InsufficientFunds(10, new int[]{}, new int[]{2, 10}),
                new InsufficientFunds(10, new int[]{1, 2, 9}, new int[]{}),
                new InsufficientFunds(10, new int[]{1, 2}, new int[]{7}),
                new InsufficientFunds(10, new int[]{}, new int[]{1, 3, 6}),
                new InsufficientFunds(10, new int[]{4, 6, 7, 8}, new int[]{}),
                new InsufficientFunds(10, new int[]{4, 8}, new int[]{6, 7}),
                new InsufficientFunds(10, new int[]{}, new int[]{4, 6, 7, 8})
        };

        // Tests for players who initially have insufficient funds.
        DoubleSpending[] doubleSpendCases = new DoubleSpending[]{
                new DoubleSpending(new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1}, new int[]{6}),
                new DoubleSpending(new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1}, new int[]{3, 10}),
                new DoubleSpending(new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1}, new int[]{1, 7, 8}),
                new DoubleSpending(new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1}, new int[]{4, 6, 7, 8})
        };

        // Set up tests for players with insufficient funds.
        for (InsufficientFunds maliciousCase : insufficientFundsCases) {

        }

        // Set up tests for players who double spend.
        for (DoubleSpending maliciousCase : doubleSpendCases) {

        }

        // Error test cases that I need blockchain make:
        // A player sends different encryption keys blockchain different players.
        // A player sends different output vectors blockchain different players.
        // A player lies about the equivocation check.
        // A player drops an address during phase 2.
        // A player drops an address and adds another one in phase 2.
        // A player drops an address and adds a duplicate in phase 2.
        // A player claims something went wrong in phase 2 when it didn't.
        // Player lies about what another player said (invalid signature).
        // A player disconnects at the wrong time.
        // Different combinations of these at the same time.

        // Run the tests.
        for (Map.Entry<Integer, testCase> test : tests.entrySet()) {
            System.out.println("running test case " + test.getKey()  + (test.getValue().description != null ? ": " + test.getValue().description : ""));

            List<Simulator.InitialState> init = new LinkedList<>();

            for (Expected  machine : test.getValue().machines.values()) {
                init.add(machine.input);
            }
            MockCrypto crypto = new MockCrypto(2222).setSigningKeyCounter(test.getValue().machines.size() + 1);

            Simulator sim = new Simulator(test.getValue().τ, test.getValue().amount, init, new MockMessageFactory(), crypto, test.getValue().coin);

            Map<SigningKey, ReturnState> errors = sim.runSimulation();

            // Check that the map of error states returned matches that which was expected.
            for (Map.Entry<SigningKey, Expected> machine : test.getValue().machines.entrySet()) {
                SigningKey key = machine.getKey();
                Expected expected = machine.getValue();
                ReturnState result = errors.get(key);

                Assert.assertNotNull(result);

                if (expected.output.success && !result.success) {
                    if (result.error != null) {
                        result.error.printStackTrace();
                    }
                    if (result.blame != null) {
                        System.out.println(result.blame.toString());
                    }
                }

                Assert.assertTrue(expected.output.match(result));

                errors.remove(key);
            }

            // I don't know why there would be any left, but just to make sure!
            Assert.assertTrue(errors.isEmpty());
        }
    }
}
