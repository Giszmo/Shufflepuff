package com.shuffle.form;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Integration tests for the protocol.
 *
 * Created by Daniel Krawisz on 12/10/15.
 */
public class TestShuffleMachine {
    public class Expected {
        Simulator.InitialState input;
        ReturnState output;

        Expected(Simulator.InitialState input, ReturnState output) {
            this.input = input;
            this.output = output;
        }
    }

    public class testCase {
        String description = null;
        SessionIdentifier τ = new MockSessionIdentifier();
        CoinAmount ν;
        Map<SigningKey, Expected> machines = new HashMap<>();

        testCase(Map<SigningKey, Expected> machines, CoinAmount ν) {
            this.ν = ν;
            this.machines = machines;
        }

        testCase(CoinAmount ν, String desc) {
            this.description = desc;
        }

        testCase(CoinAmount ν) {
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
                testCase test = new testCase(new MockCoinAmount(17), "successful run with " + players + " players.");

                MockCrypto crypto = new MockCrypto();
                MockCoin coin = new MockCoin();
                SessionIdentifier τ = new MockSessionIdentifier();
                for (int i = 0; i < players; i++) {
                    MockSigningKey key = new MockSigningKey(i);
                    test.put(new MockSigningKey(i),
                            new Expected(
                                    new Simulator.InitialState(key, new MockPacketFactory(key.VerificationKey()), crypto, coin),
                                    new ReturnState(true, τ, ShufflePhase.Completed, null)
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
            SessionIdentifier session = new MockSessionIdentifier();

            Simulator.InitialState[] init = new Simulator.InitialState[players];
            Crypto crypto = new MockCrypto();
            for (int i = 0; i < players; i++) {
                try {
                    SigningKey key = crypto.SigningKey();
                    init[i] = new Simulator.InitialState(key, crypto, new MockCoin());
                } catch (CryptographyException e) {
                    Assert.fail("unable to construct initial state.");
                }
            }

            Simulator sim = new Simulator(session, test.getValue().ν, init);

            Map<SigningKey, ReturnState> errors = sim.runSimulation();
        }
    }
}
