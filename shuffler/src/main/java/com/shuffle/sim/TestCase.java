package com.shuffle.sim;

import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.protocol.Machine;
import com.shuffle.protocol.MessageFactory;
import com.shuffle.protocol.SessionIdentifier;
import com.shuffle.protocol.blame.Matrix;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * TestCase attempts to provide a unified way of constructing test cases for different
 * implementations of CoinShuffle.
 *
 * Created by Daniel Krawisz on 3/15/16.
 */
public abstract class TestCase {
    private static Logger log = LogManager.getLogger(TestCase.class);

    public final static class Mismatch {
        public final SigningKey player;
        public final Machine.Expected expected;
        public final Machine result;

        public Mismatch(SigningKey player, Machine.Expected expected, Machine result) {
            this.player = player;
            this.expected = expected;
            this.result = result;
        }
    }

    // Returns a map containing the set of results which did not match expectations. An empty map
    // represents a successful test.
    public static Map<SigningKey, Mismatch> test(InitialState init, MessageFactory messages) {

        // Run the simulation.
        Map<SigningKey, Machine> results = Simulator.run(init, messages);

        // Get the expected values.
        Map<SigningKey, Matrix> expected = init.expectedBlame();

        // The result to be returned.
        Map<SigningKey, Mismatch> mismatch = new HashMap<>();

        // Check that the map of error states returned matches that which was expected.
        for (Map.Entry<SigningKey, Matrix> ex : expected.entrySet()) {
            SigningKey key = ex.getKey();
            Machine result = results.get(key);
            Machine.Expected expect = new Machine.Expected(init.session, null, null, ex.getValue());

            if (result == null || !expect.match(result)) {
                log.error("  expected " + expect);
                log.error("  result   " + result);
                mismatch.put(key, new Mismatch(key, expect, result));
            }
        }

        return mismatch;
    }

    private final long amount;

    private final SessionIdentifier session;

    protected TestCase(long amount, MessageFactory messages, SessionIdentifier session) {
        this.amount = amount;
        this.session = session;
    }

    protected abstract Crypto crypto();

    public final InitialState successfulTestCase(final int numPlayers) {
        return InitialState.successful(session, amount, crypto(), numPlayers);
    }

    public final InitialState insufficientFundsTestCase(
            final int numPlayers,
            final int[] deadbeats,
            final int[] poor,
            final int[] spenders) {
        return InitialState.insufficientFunds(session, amount, crypto(), numPlayers, deadbeats, poor, spenders);
    }

    public final InitialState doubleSpendTestCase(
            final int[] views,
            final int[] spenders
    ) {
        return InitialState.doubleSpend(session, amount, crypto(), views, spenders);
    }

    public final InitialState equivocateAnnouncementTestCase(
            final int numPlayers,
            final InitialState.Equivocation[] equivocators
    ) {
        return InitialState.equivocateAnnouncement(session, amount, crypto(), numPlayers, equivocators);
    }

    public final InitialState equivocateBroadcastTestCase(
            final int numPlayers,
            final int[] equivocation) {
        return InitialState.equivocateBroadcast(session, amount, crypto(), numPlayers, equivocation);
    }

    public final InitialState dropAddressTestCase(
            final int numPlayers,
            final int[][] drop,
            final int[][] replaceNew,
            final int[][] replaceDuplicate) {
        return InitialState.dropAddress(session, amount, crypto(), numPlayers, drop, replaceNew, replaceDuplicate);
    }

    public final InitialState invalidSignatureTestCase(
            final int numPlayers,
            final int[] mutants
    ) {
        return InitialState.invalidSignature(session, amount, crypto(), numPlayers, mutants);
    }
}
