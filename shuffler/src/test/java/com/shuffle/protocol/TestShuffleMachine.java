package com.shuffle.protocol;

import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.mock.MockCoin;
import com.shuffle.mock.MockCrypto;
import com.shuffle.mock.MockMessageFactory;
import com.shuffle.mock.MockSessionIdentifier;
import com.shuffle.protocol.blame.Matrix;
import com.shuffle.protocol.blame.Evidence;
import com.shuffle.protocol.blame.Reason;
import com.shuffle.sim.InitialState;
import com.shuffle.sim.Simulator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Integration tests for the protocol.
 *
 * Created by Daniel Krawisz on 12/10/15.
 */
public class TestShuffleMachine {
    private static Logger log = LogManager.getLogger(TestShuffleMachine.class);

    public class TestCase {

        String description = null;
        int id;
        SessionIdentifier session;
        long amount;
        Map<SigningKey, Machine> expected = new HashMap<>();
        Map<SigningKey, Machine> results = new HashMap<>();

        TestCase(SessionIdentifier session, long amount, String desc, int id) {
            this.session = session;
            this.description = desc;
            this.amount = amount;
            this.id = id;
        }

        TestCase put(SigningKey key, Machine ex, Machine r) {
            results.put(key, r);
            expected.put(key, ex);
            return this;
        }

        public void putSuccessfulPlayer(SigningKey key, Machine m) {
            put(key, new Machine(session, Phase.Completed, null, null), m);
        }

        public void check() {

            if(amount == 0){
                Assert.fail();
            }

            log.info("Checking test case: " + (description != null ? " " + description + "; " : "") + "case number = " + id);
            log.info("expected: " + expected.toString());
            log.info("results:  " + results.toString());

            // Check that the map of error states returned matches that which was expected.
            for (Map.Entry<SigningKey, Machine> ex : expected.entrySet()) {
                SigningKey key = ex.getKey();
                Machine result = results.get(key);
                Machine expected = ex.getValue();

                Assert.assertNotNull(result);

                Assert.assertTrue(expected.match(result));

                results.remove(key);
            }

            // I don't know why there would be any left, but just to make sure!
            Assert.assertTrue(results.isEmpty());
        }
    }

    private class Equivocation {
        final int equivocator;
        final int[] equivocation;

        private Equivocation(int equivocator, int[] equivocation) {
            // Testing the case where the first player is the equivocator is too hard for now.
            // It would require basically writing a whole new version of protocolDefinition()
            // to be a test function. It is unlikely that testing case will find a bug in the code.
            if (equivocator == 1) {
                throw new IllegalArgumentException();
            }

            for (int eq : equivocation) {
                if (eq <= equivocator) {
                    throw new IllegalArgumentException();
                }
            }

            this.equivocator = equivocator;
            this.equivocation = equivocation;
        }

        @Override
        public String toString() {
            return "equivocation[" + equivocator + ", " + Arrays.toString(equivocation) + "]";
        }
    }

    int seed = 99;

    TestCase successfulExpectation(TestCase test, Map<SigningKey, Machine> results) {
        for (SigningKey key : results.keySet()) {
            test.putSuccessfulPlayer(key, results.get(key));
        }
        return test;
    }

    // Create a test case representing a successful run.
    public TestCase SuccessfulRun(int caseNo, int numPlayer, Simulator sim) {
        SessionIdentifier session = new MockSessionIdentifier("success" + caseNo);
        MockCoin coin = new MockCoin();
        Crypto crypto = new MockCrypto(++seed);
        long amount = 17;

        return successfulExpectation(
                new TestCase(session, amount, "successful run with " + numPlayer + " players.", caseNo),
                sim.run(InitialState.successful(session, numPlayer, amount, coin, crypto), crypto)
        );
    }
    
    public TestCase InsufficientFunds(
            int caseNo,
            int numPlayers,
            int[] deadbeats,
            int[] poor,
            int[] spenders,
            Simulator sim) {

        SessionIdentifier session = new MockSessionIdentifier("fund" + caseNo);
        MockCoin coin = new MockCoin();
        long amount = 17;
        TestCase test = new TestCase(session, amount, "Insufficient funds test case.", caseNo);
        Crypto crypto = new MockCrypto(++seed);

        InitialState init = InitialState.insufficientFunds(session, numPlayers, deadbeats, poor, spenders, amount, coin, crypto);

        Map<SigningKey, Machine> results = sim.run(init, crypto);
        Map<SigningKey, Matrix> expected = init.expectedBlame();

        for(SigningKey i : results.keySet()) {

            test.put(i,
                    new Machine(session, null, null, expected.get(i)),
                    results.get(i));
        }

        return test;
    }

    public TestCase DoubleSpend(int caseNo, int[] views, int[] doubleSpenders, Simulator sim) {

        SessionIdentifier session = new MockSessionIdentifier("spend" + caseNo);
        long amount = 17;
        TestCase test = new TestCase(session, amount, "Double spending test case.", caseNo);

        Set<com.shuffle.sim.MockCoin> coinNets = new HashSet<>();
        Map<Integer, com.shuffle.sim.MockCoin> coinNetMap = new HashMap<>();
        List<com.shuffle.sim.MockCoin> coinNetList = new LinkedList<>();

        for (int view : views) {
            if (!coinNetMap.containsKey(view)) {
                com.shuffle.sim.MockCoin coin = new MockCoin();
                coinNetMap.put(view, coin);
                coinNets.add(coin);
            }
            coinNetList.add(coinNetMap.get(view));
        }

        Crypto crypto = new MockCrypto(++seed);
        InitialState init = InitialState.doubleSpend(session, coinNets, coinNetList, doubleSpenders, amount, crypto);

        Map<SigningKey, Machine> results = sim.run(init, crypto);

        // The set of offending transactions.
        Map<SigningKey, Transaction> offenders = new HashMap<>();
        Map<SigningKey, com.shuffle.sim.MockCoin> playerToCoin = new HashMap<>();

        SortedSet<SigningKey> players = new TreeSet<>();
        players.addAll(results.keySet());

        {int i = 0;
        for (SigningKey key : players) {

            com.shuffle.sim.MockCoin coin = coinNetMap.get(views[i]);
            playerToCoin.put(key, coin);

            Transaction t = coin.getConflictingTransaction(key.VerificationKey().address(), 17);

            if (t != null) {
                offenders.put(key, t);
            }

            i++;
        }}

        for (SigningKey i : results.keySet()) {
            Matrix bm;
            Phase phase;

            if (offenders.containsKey(i)) {
                bm = InitialState.anyMatrix;
                phase = null;
            } else {
                bm = new Matrix();
                phase = Phase.Blame;
                for (SigningKey j : results.keySet()) {
                    for (SigningKey k : results.keySet()) {

                        if (offenders.containsKey(j)) {
                            // We don't care who the malicious players accuse.
                            bm.put(j.VerificationKey(), k.VerificationKey(), InitialState.anyReason);
                        } else if(offenders.containsKey(k)) {
                            if (playerToCoin.get(j) == playerToCoin.get(k)) {
                                // Only include the transaction if the player has the same view
                                // of the network as the double spender.
                                bm.put(j.VerificationKey(), k.VerificationKey(),
                                        Evidence.DoubleSpend(true, offenders.get(k)));
                            }
                        }
                    }
                }
            }

            test.put(i,
                    new Machine(session, phase, null, bm),
                    results.get(i));
        }

        return test;
    }

    // Run a test case for equivocation during phase 1.
    public TestCase EquivocateAnnouncement(
            int caseNo,
            int numPlayers,
            Equivocation[] equivocators,
            Simulator sim
    ) {
        long amount = 17;
        SessionIdentifier session = new MockSessionIdentifier("eqv" + caseNo);
        InitialState init = new InitialState(session, amount).defaultCoin(new MockCoin());

        Crypto crypto = new MockCrypto(++seed);

        int eq = 0;
        for (int i = 1; i <= numPlayers; i ++) {
            SigningKey key = crypto.makeSigningKey();
            init.player(key).initialFunds(20);

            while(eq < equivocators.length && equivocators[eq].equivocator < i) {
                eq++;
            }

            if (eq < equivocators.length && equivocators[eq].equivocator == i) {
                init.equivocateAnnouncement(equivocators[eq].equivocation);
            }
        }

        log.info("Announcement equivocation test case: " + Arrays.toString(equivocators));

        TestCase test = new TestCase(session, amount, "Announcement phase equivocation test case.", caseNo);
        Map<SigningKey, Machine> results = sim.run(init, crypto);
        Map<SigningKey, Matrix> expected = init.expectedBlame();

        for(SigningKey i : results.keySet()) {

            test.put(i,
                    new Machine(session, null, null, expected.get(i)),
                    results.get(i));
        }

        return test;
    }

    // Run a test case for equivocation during phase 3.
    public TestCase EquivocateOutput(int caseNo, int numPlayers, int[] equivocation, Simulator sim) {
        long amount = 17;
        SessionIdentifier session = new MockSessionIdentifier("eqv" + caseNo);
        InitialState init = new InitialState(session, amount).defaultCoin(new MockCoin());

        Crypto crypto = new MockCrypto(++seed);

        // Only the last player can equivocate.
        for (int i = 1; i < numPlayers; i ++) {
            init.player(crypto.makeSigningKey()).initialFunds(20);
        }

        // Add the malicious equivocator.
        init.player(crypto.makeSigningKey()).initialFunds(20).equivocateOutputVector(equivocation);

        log.info("Broadcast equivocation test case: " + Arrays.toString(equivocation));

        TestCase test = new TestCase(session, amount, "Broadcast phase equivocation test case.", caseNo);
        Map<SigningKey, Machine> results = sim.run(init, crypto);
        Map<SigningKey, Matrix> expected = init.expectedBlame();

        for (SigningKey i : results.keySet()) {
            test.put(i,
                    new Machine(session, null, null, expected.get(i)),
                    results.get(i));
        }

        return test;
    }

    public TestCase InvalidTransactionSignature(int caseNo, int numPlayers, int[] weirdos, Simulator sim) {
        Set<Integer> class2 = new HashSet<>();

        for (int weirdo : weirdos) {
            class2.add(weirdo);
        }

        MockCoin coin1 = new MockCoin().setZ(1);
        MockCoin coin2 = new MockCoin().setZ(2);

        Crypto crypto = new MockCrypto(++seed);

        List<com.shuffle.sim.MockCoin> coins = new LinkedList<>();
        coins.add(coin1);
        coins.add(coin2);

        long amount = 17;
        SessionIdentifier session = new MockSessionIdentifier("sig" + caseNo);
        InitialState init = new InitialState(session, amount).coins(coins);

        for (int i = 1; i <= numPlayers; i ++) {
            if (class2.contains(i)) {
                init.player(crypto.makeSigningKey()).initialFunds(20).coin(coin2);
            } else {
                init.player(crypto.makeSigningKey()).initialFunds(20).coin(coin1);
            }
        }

        TestCase test = new TestCase(session, amount, "invalid signature test case.", caseNo);
        Map<SigningKey, Machine> results = sim.run(init, crypto);
        SortedSet<SigningKey> players = new TreeSet<>();
        players.addAll(results.keySet());

        // Results should be that every player blames all players in the other class.
        Set<SigningKey> keyClass2 = new HashSet<>();
        int index = 1;
        for (SigningKey key : players) {
            if (class2.contains(index)) {
                keyClass2.add(key);
            }
            index ++;
        }

        for (Map.Entry<SigningKey, Machine> result : results.entrySet()) {
            SigningKey i = result.getKey();
            Machine returnState = result.getValue();

            Matrix bm = null;

            if (class2.contains(i)) { // <-- TODO this is definitely an error but I don't know what should go here.
                bm = InitialState.anyMatrix;
            } else {

                bm = new Matrix();

                for (SigningKey j : results.keySet()) {
                    for (SigningKey k : results.keySet()) {
                        boolean inClass2 = keyClass2.contains(j);
                        if (inClass2 != keyClass2.contains(k)) {
                            bm.put(j.VerificationKey(), k.VerificationKey(),
                                    Evidence.Expected(Reason.InvalidSignature, inClass2 == keyClass2.contains(i)));
                        }
                    }
                }
            }

            test.put(i, new Machine(session, Phase.Blame, null, bm), returnState);
        }

        return test;
    }

    class Dropper {
        final int player;
        final int drop;
        final int duplicate;

        Dropper(int player, int drop, int duplicate) {
            this.player = player;
            this.drop = drop;
            this.duplicate = duplicate;
        }

        @Override
        public String toString() {
            return " drop " + drop;
        }
    }

    public TestCase DropAddress(int caseNo, int numPlayers, Dropper dropper, Simulator sim) {

        long amount = 17;
        SessionIdentifier session = new MockSessionIdentifier("drop" + caseNo);
        InitialState init = new InitialState(session, amount).defaultCoin(new MockCoin());

        Crypto crypto = new MockCrypto(++seed);

        // Set a player to drop an address.
        for (int i = 1; i <= numPlayers; i ++) {
            init.player(crypto.makeSigningKey()).initialFunds(20);

            if (i == dropper.player) {
                if (dropper.duplicate > 0) {
                    init.replace(dropper.drop, dropper.duplicate);
                } else {
                    init.drop(dropper.drop);
                }
            }
        }

        log.info("drop address test case: " + dropper.toString());

        TestCase test = new TestCase(session, amount, "Drop address test case.", caseNo);
        Map<SigningKey, Machine> results = sim.run(init, crypto);

        // Find malicious player.

        // Construct expected matrix.

        return null; // TODO
    }

    public TestCase DropAddressReplaceNew(int caseNo, int numPlayers, int drop, Simulator sim) {
        return null; // TODO
    }

    @Test
    // Tests for successful runs of the protocol.
    public void testSuccess() {
        Simulator sim = new Simulator(new MockMessageFactory());

        // Tests for successful runs.
        int caseNo = 0;
        int minPlayers = 2;
        int maxPlayers = 12;
        for (int numPlayer = minPlayers; numPlayer <= maxPlayers; numPlayer++) {
            try {
                SuccessfulRun(caseNo, numPlayer, sim).check();
                caseNo++;
            } catch (CryptographyError e) {
                Assert.fail("could not create test case " + caseNo);
            }
        }
    }

    @Test
    // Tests for players who come in without enough cash.
    public void testInsufficientFunds() {
        Simulator sim = new Simulator(new MockMessageFactory());
        int caseNo = 0;

        // Tests for players who initially have insufficient funds.
        InsufficientFunds(caseNo++, 2,  new int[]{1}, new int[]{}, new int[]{}, sim).check();
        InsufficientFunds(caseNo++, 2,  new int[]{}, new int[]{1}, new int[]{}, sim).check();
        InsufficientFunds(caseNo++, 2,  new int[]{}, new int[]{}, new int[]{1}, sim).check();
        InsufficientFunds(caseNo++, 2,  new int[]{1, 2}, new int[]{}, new int[]{}, sim).check();
        InsufficientFunds(caseNo++, 3,  new int[]{1}, new int[]{}, new int[]{}, sim).check();
        InsufficientFunds(caseNo++, 5,  new int[]{3}, new int[]{}, new int[]{}, sim).check();
        InsufficientFunds(caseNo++, 5,  new int[]{}, new int[]{4}, new int[]{}, sim).check();
        InsufficientFunds(caseNo++, 5,  new int[]{}, new int[]{}, new int[]{5}, sim).check();
        InsufficientFunds(caseNo++, 10, new int[]{5, 10}, new int[]{}, new int[]{}, sim).check();
        InsufficientFunds(caseNo++, 10, new int[]{}, new int[]{1, 2}, new int[]{}, sim).check();
        InsufficientFunds(caseNo++, 10, new int[]{}, new int[]{}, new int[]{3, 5}, sim).check();
        InsufficientFunds(caseNo++, 10, new int[]{5}, new int[]{10}, new int[]{}, sim).check();
        InsufficientFunds(caseNo++, 10, new int[]{}, new int[]{3}, new int[]{9}, sim).check();
        InsufficientFunds(caseNo,   10, new int[]{1}, new int[]{}, new int[]{2}, sim).check();
    }

    @Test
    // Tests for malicious players who send different output vectors to different players.
    public void testEquivocationBroadcast() {
        Simulator sim = new Simulator(new MockMessageFactory());
        int caseNo = 0;

        // A player sends different output vectors to different players.
        EquivocateOutput(caseNo++, 3, new int[]{1}, sim).check();
        EquivocateOutput(caseNo++, 3, new int[]{2}, sim).check();
        EquivocateOutput(caseNo++, 4, new int[]{1}, sim).check();
        EquivocateOutput(caseNo++, 4, new int[]{1, 2}, sim).check();
        EquivocateOutput(caseNo, 10, new int[]{3, 5, 7}, sim).check();
    }

    @Test
    public void testEquivocationAnnounce() {
        Simulator sim = new Simulator(new MockMessageFactory());
        int caseNo = 0;

        // A player sends different encryption keys to different players.
        EquivocateAnnouncement(caseNo++, 3,
                new Equivocation[]{
                        new Equivocation(2, new int[]{3})}, sim).check();
        EquivocateAnnouncement(caseNo++, 5,
                new Equivocation[]{
                        new Equivocation(2, new int[]{4, 5})}, sim).check();
        EquivocateAnnouncement(caseNo++, 10,
                new Equivocation[]{
                        new Equivocation(2, new int[]{4, 10}),
                        new Equivocation(5, new int[]{7, 8})}, sim).check();
        EquivocateAnnouncement(caseNo, 10,
                new Equivocation[]{
                        new Equivocation(2, new int[]{3}),
                        new Equivocation(4, new int[]{5, 6}),
                        new Equivocation(8, new int[]{9})}, sim).check();
    }

    @Test
    public void testTransactionDisagreement() {
        Simulator sim = new Simulator(new MockMessageFactory());
        int caseNo = 0;

        // Player generates a different transaction signature to everyone else.
        InvalidTransactionSignature(caseNo++, 2, new int[]{2}, sim).check();
        InvalidTransactionSignature(caseNo++, 5, new int[]{2}, sim).check();
        InvalidTransactionSignature(caseNo++, 5, new int[]{2, 3}, sim).check();
        InvalidTransactionSignature(caseNo, 10, new int[]{2, 5, 6, 7}, sim).check();
    }

    @Test
    public void testDoubleSpending() {
        Simulator sim = new Simulator(new MockMessageFactory());
        int caseNo = 0;

        // Tests for players who spend funds while the protocol is going on.
        DoubleSpend(caseNo++, new int[]{0, 0}, new int[]{1}, sim).check();
        DoubleSpend(caseNo++, new int[]{0, 1, 0}, new int[]{1}, sim).check();
        DoubleSpend(caseNo++, new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1}, new int[]{6}, sim).check();
        DoubleSpend(caseNo++, new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1}, new int[]{3, 10}, sim).check();
        DoubleSpend(caseNo++, new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1}, new int[]{1, 7, 8}, sim).check();
        DoubleSpend(caseNo, new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1}, new int[]{4, 6, 7, 8}, sim).check();

    }

    @Test
    // Tests for failures during the shuffle phase.
    public void testShuffleMalice() {
        Simulator sim = new Simulator(new MockMessageFactory());
        int caseNo = 0;

        // A player drops an address during phase 2.


        // A player drops an address and adds another one in phase 2.


        // A player drops an address and adds a duplicate in phase 2.
        Assert.fail();
    }

    @Test
    public void testLies() {

        // A player lies about the equivocation check.
        // A player claims something went wrong in phase 2 when it didn't.
        Assert.fail();
    }

    @Test
    public void testInvalidSignature() {
        // Test that a player is excluded for making an invalid signature.
        Assert.fail();
    }

    @Test
    // Players disconnect at different points during the protocol.
    // TODO must include cases in which a malicious player disconnects after sending a malicious message!!
    public void testDisconnect() {
        Assert.fail();
    }
}
