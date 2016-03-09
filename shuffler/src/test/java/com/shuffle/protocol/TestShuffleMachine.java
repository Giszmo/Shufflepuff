package com.shuffle.protocol;

import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.mock.MockCrypto;
import com.shuffle.mock.MockMessageFactory;
import com.shuffle.mock.MockSessionIdentifier;
import com.shuffle.protocol.blame.Matrix;
import com.shuffle.sim.InitialState;
import com.shuffle.sim.Simulator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
                log.info("  expected: " + expected.toString());
                log.info("  results:  " + result.toString());

                Assert.assertNotNull(result);

                Assert.assertTrue(expected.match(result));

                results.remove(key);
            }

            // I don't know why there would be any left, but just to make sure!
            Assert.assertTrue(results.isEmpty());
        }
    }

    private abstract class TestCaseFactory {
        protected abstract InitialState initialState(SessionIdentifier session, long amount, Crypto crypto);

        final protected TestCase construct(String type, int caseNo, Simulator sim) {


            SessionIdentifier session = new MockSessionIdentifier("" + caseNo);
            long amount = 17;
            TestCase test = new TestCase(session, amount, type, caseNo);
            Crypto crypto = new MockCrypto(++seed);

            InitialState init = initialState(session, amount, crypto);

            Map<SigningKey, Machine> results = sim.run(init, crypto);
            Map<SigningKey, Matrix> expected = init.expectedBlame();

            for(SigningKey i : expected.keySet()) {

                test.put(i,
                        new Machine(session, null, null, expected.get(i)),
                        results.get(i));
            }

            return test;
        }
    }

    private class SuccessfulTestCase extends TestCaseFactory {
        final int numPlayers;

        public SuccessfulTestCase(int numPlayers) {
            this.numPlayers = numPlayers;
        }

        @Override
        protected InitialState initialState(SessionIdentifier session, long amount, Crypto crypto) {
            return InitialState.successful(session, numPlayers, amount, crypto);
        }
    }

    private class InsufficientFundsTestCase extends TestCaseFactory {
        final int numPlayers;
        int[] deadbeats;
        int[] poor;
        int[] spenders;

        public InsufficientFundsTestCase(
                int numPlayers,
                int[] deadbeats,
                int[] poor,
                int[] spenders) {
            this.numPlayers = numPlayers;
            this.deadbeats = deadbeats;
            this.poor = poor;
            this.spenders = spenders;
        }

        @Override
        protected InitialState initialState(SessionIdentifier session, long amount, Crypto crypto) {
            return InitialState.insufficientFunds(session, numPlayers, deadbeats, poor, spenders, amount, crypto);
        }
    }

    private class DoubleSpendTestCase extends TestCaseFactory {
        final int[] views;
        final Set<Integer> doubleSpenders = new HashSet<>();

        public DoubleSpendTestCase(int[] views, int[] doubleSpenders) {
            this.views = views;

            for (int d : doubleSpenders) {
                this.doubleSpenders.add(d);
            }
        }

        @Override
        protected InitialState initialState(SessionIdentifier session, long amount, Crypto crypto) {

            InitialState init = new InitialState(session, amount);
            for (int i = 1; i < views.length; i ++) {
                init.player(crypto.makeSigningKey()).initialFunds(20).networkPoint(views[i]);

                if (doubleSpenders.contains(i)) {
                    init.doubleSpend(13);
                }
            }
            return init;
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

    private class EquivocateAnnouncementTestCase extends TestCaseFactory {
        final int numPlayers;
        final Equivocation[] equivocators;

        public EquivocateAnnouncementTestCase(int numPlayers, Equivocation[] equivocators) {
            this.numPlayers = numPlayers;
            this.equivocators = equivocators;
        }

        @Override
        protected InitialState initialState(SessionIdentifier session, long amount, Crypto crypto) {
            InitialState init = new InitialState(session, amount);

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

            return init;
        }
    }

    private class EquivocateOutputTestCase extends TestCaseFactory {
        final int numPlayers;
        final int[] equivocation;

        public EquivocateOutputTestCase(int numPlayers, int[] equivocation) {
            this.numPlayers = numPlayers;
            this.equivocation = equivocation;
        }

        @Override
        protected InitialState initialState(SessionIdentifier session, long amount, Crypto crypto) {
            InitialState init = new InitialState(session, amount);

            // Only the last player can equivocate.
            for (int i = 1; i < numPlayers; i ++) {
                init.player(crypto.makeSigningKey()).initialFunds(20);
            }

            // Add the malicious equivocator.
            init.player(crypto.makeSigningKey()).initialFunds(20).equivocateOutputVector(equivocation);

            return init;
        }
    }

    private class DropTestCase extends TestCaseFactory {
        final int numPlayers;

        Map<Integer, Integer> drop = new HashMap<>();
        Map<Integer, Integer> replaceNew = new HashMap<>();
        Map<Integer, Integer[]> replaceDuplicate = new HashMap<>();

        public DropTestCase(int numPlayers, int[][] drop, int[][] replaceNew, int[][] replaceDuplicate) {
            this.numPlayers = numPlayers;

            if (drop != null) {
                for (int[] d : drop) {
                    if (d.length == 2 && d[1] < d[0]) {
                        this.drop.put(d[0], d[1]);
                    }
                }
            }

            if (replaceDuplicate != null) {
                for (int[] d : replaceDuplicate) {
                    if (d.length == 2 && d[1] < d[0]) {
                        this.replaceDuplicate.put(d[0], new Integer[]{d[1], d[2]});
                    }
                }
            }

            if (replaceNew != null) {
                for (int[] d : replaceNew) {
                    if (d.length == 2 && d[1] < d[0]) {
                        this.replaceNew.put(d[0], d[1]);
                    }
                }
            }
        }

        @Override
        protected InitialState initialState(SessionIdentifier session, long amount, Crypto crypto) {
            InitialState init = new InitialState(session, amount);

            for (int i = 1; i <= numPlayers; i ++) {

                init.player(crypto.makeSigningKey()).initialFunds(20);

                if (drop.containsKey(i)) {
                    init.drop(drop.get(i));
                } else if (replaceDuplicate.containsKey(i)) {
                    Integer[] dup = replaceDuplicate.get(i);
                    init.replace(dup[0], dup[1]);
                } else if (replaceNew.containsKey(i)) {
                    init.replace(replaceNew.get(i));
                }
            }

            return init;
        }
    }

    private class InvalidTransactionSignatureTestCase extends TestCaseFactory {
        final int numPlayers;
        final Set<Integer> mutants = new HashSet<>();

        public InvalidTransactionSignatureTestCase(int numPlayers, int[] mutants) {
            this.numPlayers = numPlayers;

            for (int mutant : mutants) {
                this.mutants.add(mutant);
            }
        }

        @Override
        protected InitialState initialState(SessionIdentifier session, long amount, Crypto crypto) {
            InitialState init = new InitialState(session, amount);

            for (int i = 1; i <= numPlayers; i ++) {
                init.player(crypto.makeSigningKey()).initialFunds(20);

                if(mutants.contains(i)) {
                    init.mutateTransaction();
                }
            }

            return init;
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
        return new SuccessfulTestCase(numPlayer).construct("successful run with " + numPlayer + " players.", caseNo, sim);
    }
    
    public TestCase InsufficientFunds(
            int caseNo,
            int numPlayers,
            int[] deadbeats,
            int[] poor,
            int[] spenders,
            Simulator sim) {

        return new InsufficientFundsTestCase(numPlayers, deadbeats, poor, spenders).construct("Insufficient funds test case.", caseNo, sim);
    }

    public TestCase DoubleSpend(int caseNo, int[] views, int[] doubleSpenders, Simulator sim) {
        return new DoubleSpendTestCase(views, doubleSpenders).construct("Double spend test case.", caseNo, sim);
    }

    // Run a test case for equivocation during phase 1.
    public TestCase EquivocateAnnouncement(
            int caseNo,
            int numPlayers,
            Equivocation[] equivocators,
            Simulator sim
    ) {
        return new EquivocateAnnouncementTestCase(numPlayers, equivocators).construct("Announcement phase equivocation test case.", caseNo, sim);
    }

    // Run a test case for equivocation during phase 3.
    public TestCase EquivocateOutput(int caseNo, int numPlayers, int[] equivocation, Simulator sim) {
        return new EquivocateOutputTestCase(numPlayers, equivocation).construct("Broadcast phase equivocation test case.", caseNo, sim);
    }

    // Run a test case for a player who drops an address in phase 2.
    public TestCase DropAddress(int caseNo, int numPlayers, int[][] drop, int[][] replaceNew, int[][] replaceDuplicate, Simulator sim) {
        return new DropTestCase(numPlayers, drop, replaceNew, replaceDuplicate).construct("Shuffle phase mischief test case.", caseNo, sim);
    }

    public TestCase InvalidTransactionSignature(int caseNo, int numPlayers, int[] mutants, Simulator sim) {
        return new InvalidTransactionSignatureTestCase(numPlayers, mutants).construct("invalid transaction signature test case", caseNo, sim);
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
    // Tests for failures during the shuffle phase.
    public void testShuffleMalice() {
        Simulator sim = new Simulator(new MockMessageFactory());
        int caseNo = 0;

        // A player drops an address during phase 2.
        DropAddress(caseNo++, 3, new int[][]{new int[]{2, 1}}, null, null, sim).check();
        DropAddress(caseNo++, 3, new int[][]{new int[]{3, 2}}, null, null, sim).check();
        DropAddress(caseNo++, 4, new int[][]{new int[]{3, 2}}, null, null, sim).check();

        // A player drops an address and adds another one in phase 2.
        DropAddress(caseNo++, 3, null, new int[][]{new int[]{2, 1}}, null, sim).check();
        DropAddress(caseNo++, 3, null, new int[][]{new int[]{3, 2}}, null, sim).check();
        DropAddress(caseNo++, 4, null, new int[][]{new int[]{3, 2}}, null, sim).check();

        // A player drops an address and adds a duplicate in phase 2.
        DropAddress(caseNo++, 4, null, null, new int[][]{new int[]{3, 1, 2}}, sim).check();
        DropAddress(caseNo++, 4, null, null, new int[][]{new int[]{4, 3, 2}}, sim).check();
        DropAddress(caseNo++, 5, null, null, new int[][]{new int[]{4, 3, 2}}, sim).check();
    }

    // TODO make these work.
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

    /*@Test
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

    }*/

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
