package com.shuffle.protocol;

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
        com.shuffle.protocol.Simulator.InitialState input;
        ReturnState output;

        Expected(Simulator.InitialState input, ReturnState output) {
            this.input = input;
            this.output = output;
        }
    }

    public class testCase {
        String description = null;
        SessionIdentifier τ;
        Coin.CoinAmount ν;
        Coin coin;
        Map<SigningKey, Expected> machines = new HashMap<>();

        testCase(SessionIdentifier τ, Coin.CoinAmount ν, Coin coin, String desc) {
            this.τ = τ;
            this.description = desc;
            this.ν = ν;
            this.coin = coin;
        }

        testCase(SessionIdentifier τ, Coin.CoinAmount ν, Coin coin) {
            this.τ = τ;
            this.ν = ν;
            this.coin = coin;
        }

        testCase put(SigningKey key, Expected ex) {
            machines.put(key, ex);
            return this;
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
                testCase test = new testCase(τ, new MockCoinAmount(17), coin, "successful run with " + players + " players.");

                for (int i = 1; i <= players; i++) {
                    MockSigningKey key = new MockSigningKey(i);
                    coin.put(key.VerificationKey().address(), new MockCoin.Output(new MockCoinAmount(20), false));
                    test.put(new MockSigningKey(i),
                            new Expected(
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

        // Error test cases:
        // Player has insufficient funds.
        // A player spends his funds while the protocol is going on.
        // A player lies about another having insufficient funds.
        // A player sends different encryption keys to different players.
        // A player sends different output vectors to different players.
        // A player lies about the equivocation check.
        // A player drops an address during phase 2.
        // A player drops an address and adds another one.
        // A player drops an address and adds a duplicate.
        // Different combinations of these at the same time.

        // Run the tests.
        for (Map.Entry<Integer, testCase> test : tests.entrySet()) {
            System.out.println("running test case " + test.getKey()  + (test.getValue().description != null ? ": " + test.getValue().description : ""));

            List<Simulator.InitialState> init = new LinkedList<>();

            for (Expected  machine : test.getValue().machines.values()) {
                init.add(machine.input);
            }
            MockCrypto crypto = new MockCrypto(2222).setSigningKeyCounter(test.getValue().machines.size() + 1);

            Simulator sim = new Simulator(test.getValue().τ, test.getValue().ν, init, new MockMessageFactory(), crypto, test.getValue().coin);

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
