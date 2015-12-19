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
        CoinAmount ν;
        Coin coin;
        Map<SigningKey, Expected> machines = new HashMap<>();

        testCase(SessionIdentifier τ, CoinAmount ν, Coin coin, String desc) {
            this.τ = τ;
            this.description = desc;
            this.ν = ν;
            this.coin = coin;
        }

        testCase(SessionIdentifier τ, CoinAmount ν, Coin coin) {
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
                    coin.put(key.VerificationKey(), new MockCoin.Output(new MockCoinAmount(20), false));
                    test.put(new MockSigningKey(i),
                            new Expected(
                                    new Simulator.InitialState(key),
                                    new ReturnState(true, τ, ShufflePhase.Completed, null, null)
                            ));
                }
                tests.put(caseNo, test);
                caseNo ++;
            } catch (CryptographyException e) {
                Assert.fail("could not create test case " + caseNo);
            }
        }

        // Tests for successful runs.
        for (Map.Entry<Integer, testCase> test : tests.entrySet()) {
            System.out.println("running test case " + test.getKey() + ": " + test.getValue().description);

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
                    if (result.exception != null) {
                        result.exception.printStackTrace();
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
