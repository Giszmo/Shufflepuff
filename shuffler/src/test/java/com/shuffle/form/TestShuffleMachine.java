package com.shuffle.form;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by Daniel Krawisz on 12/10/15.
 */
public class TestShuffleMachine {

    @Test
    public void testShuffleMachine() {

        // Tests for successful runs.
        for (int players = 3; players <= 7; players ++ ) {
            SessionIdentifier session = new MockSessionIdentifier();
            CoinAmount amount = new MockCoinAmount(17);

            Simulator.InitialState[] init = new Simulator.InitialState[players];
            Crypto crypto = new MockCrypto();
            for (int i = 0; i < players; i++) {
                try {
                    SigningKey key = crypto.SigningKey();
                    init[i] = new Simulator.InitialState(key, new MockPacketFactory(key.VerificationKey()), crypto, new MockCoin());
                } catch (CryptographyException e) {
                    Assert.fail("unable to construct initial state.");
                }
            }

            Simulator sim = new Simulator(session, amount, init);

            List<ShuffleErrorState> errors = sim.runSimulation();
        }
    }
}
