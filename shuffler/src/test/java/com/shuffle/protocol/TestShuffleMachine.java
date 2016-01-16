package com.shuffle.protocol;

import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.Transaction;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Integration tests for the protocol.
 *
 * Created by Daniel Krawisz on 12/10/15.
 */
public class TestShuffleMachine {

    // A blame matrix that matches any matrix given to it.
    // Used for ensuring a test can't fail no matter what value
    // simulated adversaries return, since we only care about testing the response of the
    // honest players.
    public static class BlameMatrixPatternAny extends BlameMatrix {

        @Override
        public boolean match(BlameMatrix bm) {
            return true;
        }

        @Override
        public String toString() {
            return "Any";
        }
    }

    public static class BlameEvidencePatternAny extends BlameMatrix.BlameEvidence {
        @Override
        public boolean match(BlameMatrix.BlameEvidence e) {
            return true;
        }

        @Override
        public String toString() {
            return "Any";
        }
    }

    public static class ReturnStatePatternOr extends ReturnState {
        ReturnState a;
        ReturnState b;

        public ReturnStatePatternOr(boolean success, SessionIdentifier session, Phase phase, Throwable error, BlameMatrix blame) {
            super(success, session, phase, error, blame);
        }

        @Override
        public boolean match(ReturnState x) {
            return a.match(x) || b.match(x);
        }
    }

    public class MutateTransactionSignature implements Simulator.MessageReplacement {

        @Override
        public Packet replace(Packet packet) throws FormatException {
            if (packet.phase == Phase.VerificationAndSubmission) {
                if (packet.message instanceof MockMessage) {
                    MockMessage mockMessage = (MockMessage)packet.message;

                    MockMessage.Atom atom = mockMessage.atoms.peek();
                    if (atom.sig instanceof MockSignature) {
                        MockSignature mockSig = (MockSignature)atom.sig;
                        if (mockSig.t instanceof MockCoin.MockTransaction) {

                            MockCoin.MockTransaction mt = (MockCoin.MockTransaction) mockSig.t;
                            MockCoin.MockTransaction nmt = mt.copy();
                            nmt.z = 2;
                            mockSig.t = nmt;
                        }
                    }
                }
            }

            return packet;
        }
    }

    BlameMatrixPatternAny anyMatrix = new BlameMatrixPatternAny();
    BlameEvidencePatternAny anyReason = new BlameEvidencePatternAny();

    public class TestCase {

        String description = null;
        int id;
        SessionIdentifier session;
        long amount;
        Map<SigningKey, ReturnState> expected = new HashMap<>();
        Map<SigningKey, ReturnState> results = new HashMap<>();

        TestCase(SessionIdentifier session, long amount, String desc, int id) {
            this.session = session;
            this.description = desc;
            this.amount = amount;
            this.id = id;
        }

        TestCase put(SigningKey key, ReturnState ex, ReturnState r) {
            results.put(key, r);
            expected.put(key, ex);
            return this;
        }

        public void putSuccessfulPlayer(SigningKey key, ReturnState r) {
            put(key, new ReturnState(true, session, Phase.Completed, null, null), r);
        }

        public void check() {

            if(amount == 0){
                Assert.fail();
            }

            // Check that the map of error states returned matches that which was expected.
            for (Map.Entry<SigningKey, ReturnState> ex : expected.entrySet()) {
                SigningKey key = ex.getKey();
                ReturnState result = results.get(key);
                ReturnState expected = ex.getValue();

                Assert.assertNotNull(result);

                Assert.assertTrue(expected.match(result));

                results.remove(key);
            }

            // I don't know why there would be any left, but just to make sure!
            Assert.assertTrue(results.isEmpty());
        }
    }

    TestCase successfulExpectation(TestCase test, Map<SigningKey, ReturnState> results) {
        for (SigningKey key : results.keySet()) {
            test.putSuccessfulPlayer(key, results.get(key));
        }
        return test;
    }

    // Create a test case representing a successful run.
    public void SuccessfulRun(int caseNo, int numPlayer, Simulator sim) {
        SessionIdentifier session = new MockSessionIdentifier("success" + caseNo);
        MockCoin coin = new MockCoin();
        long amount = 17;

        successfulExpectation(
                new TestCase(session, amount, "successful run with " + numPlayer + " players.", caseNo),
                sim.successfulRun(session, numPlayer, amount, coin)
        ).check();
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

        Map<SigningKey, ReturnState> results =
                sim.insufficientFundsRun(session, numPlayers, deadbeats, poor, spenders, amount, coin);

        // If no offenders were defined, then this should be a successful run.
        if (deadbeats.length == 0 && poor.length == 0 && spenders.length == 0) {
            return successfulExpectation(test, results);
        }

        // The set of offending transactions.
        Map<SigningKey, Transaction> offenders = new HashMap<>();
        Set<SigningKey> deadbeatPlayers = new HashSet<>();

        // Get transactions by key.
        for (SigningKey key : results.keySet()) {

            Transaction t = coin.getConflictingTransaction(key.VerificationKey().address(), 17);

            if (t != null) {
                offenders.put(key, t);
            } else if (coin.valueHeld(key.VerificationKey().address()) == 0) {
                deadbeatPlayers.add(key);
            }
        }

        for (SigningKey i : results.keySet()) {
            BlameMatrix bm = null;
            Phase phase;

            if (offenders.containsKey(i) || deadbeatPlayers.contains(i)) {
                bm = anyMatrix;
            } else {

                bm = new BlameMatrix();

                for (SigningKey j : results.keySet()) {
                    for (SigningKey k : results.keySet()) {
                        if (offenders.containsKey(j) || deadbeatPlayers.contains(j)) {
                            // We don't care who the malicious players accuse.
                            bm.put(j.VerificationKey(), k.VerificationKey(), anyReason);
                        } else if(deadbeatPlayers.contains(k)) {
                            bm.put(j.VerificationKey(), k.VerificationKey(),
                                    new BlameMatrix.BlameEvidence(BlameMatrix.BlameReason.NoFundsAtAll, true));
                        } else if(offenders.containsKey(k)) {
                            bm.put(j.VerificationKey(), k.VerificationKey(),
                                    new BlameMatrix.BlameEvidence(BlameMatrix.BlameReason.InsufficientFunds, true, offenders.get(k)));
                        }
                    }
                }
            }

            test.put(i,
                    new ReturnState(false, session, Phase.Blame, null, bm),
                    results.get(i));
        }
        
        return test;
    }

    public TestCase DoubleSpend(int caseNo, int[] views, int[] doubleSpenders, Simulator sim) {

        SessionIdentifier session = new MockSessionIdentifier("spend" + caseNo);
        long amount = 17;
        TestCase test = new TestCase(session, amount, "Double spending test case.", caseNo);

        Set<Simulator.MockCoin> coinNets = new HashSet<>();
        Map<Integer, Simulator.MockCoin> coinNetMap = new HashMap<>();
        List<Simulator.MockCoin> coinNetList = new LinkedList<>();

        for (int i = 0; i < views.length; i ++) {
            if (!coinNetMap.containsKey(views[i])) {
                Simulator.MockCoin coin = new MockCoin();
                coinNetMap.put(views[i], coin);
                coinNets.add(coin);
            }
            coinNetList.add(coinNetMap.get(views[i]));
        }

        LinkedHashMap<SigningKey, ReturnState> results =
                sim.doubleSpendingRun(session, coinNets, coinNetList, doubleSpenders, amount);

        // The set of offending transactions.
        Map<SigningKey, Transaction> offenders = new HashMap<>();
        Map<SigningKey, Simulator.MockCoin> playerToCoin = new HashMap<>();

        {int i = 0;
        for (SigningKey key : results.keySet()) {

            Simulator.MockCoin coin = coinNetMap.get(views[i]);
            playerToCoin.put(key, coin);

            Transaction t = coin.getConflictingTransaction(key.VerificationKey().address(), 17);

            if (t != null) {
                offenders.put(key, t);
            }

            i++;
        }}

        for (SigningKey i : results.keySet()) {
            BlameMatrix bm = null;
            Phase phase;

            if (offenders.containsKey(i)) {
                bm = anyMatrix;
                phase = null;
            } else {
                bm = new BlameMatrix();
                phase = Phase.Blame;
                for (SigningKey j : results.keySet()) {
                    for (SigningKey k : results.keySet()) {

                        if (offenders.containsKey(j)) {
                            // We don't care who the malicious players accuse.
                            bm.put(j.VerificationKey(), k.VerificationKey(), anyReason);
                        } else if(offenders.containsKey(k)) {
                            if (playerToCoin.get(j) == playerToCoin.get(k)) {
                                // Only include the transaction if the player has the same view
                                // of the network as the double spender.
                                bm.put(j.VerificationKey(), k.VerificationKey(),
                                        new BlameMatrix.BlameEvidence(BlameMatrix.BlameReason.DoubleSpend, true, offenders.get(k)));
                            }
                        }
                    }
                }
            }

            test.put(i,
                    new ReturnState(false, session, phase, null, bm),
                    results.get(i));
        }

        return test;
    }

    // Run a test case for equivocatino during phase 1.
    public TestCase EquivocateAnnouncement(
            int caseNo,
            int numPlayers,
            Equivocation[] equivocators,
            Simulator sim
    ) {
       return null; // TODO
    }

    // Run a test case for equivocation during phase 3.
    public TestCase EquivocateOutput(int caseNo, int numPlayers, int[] eqivocation, Simulator sim) {


        return null; // TODO
    }

    @Test
    public void testSuccess() {
        MockCrypto crypto = new MockCrypto(45);
        Simulator sim = new Simulator(new MockMessageFactory(), crypto);

        // Tests for successful runs.
        int caseNo = 0;
        for (int numPlayer = 2; numPlayer <= 12; numPlayer++) {
            try {
                SuccessfulRun(caseNo, numPlayer, sim);
                caseNo++;
            } catch (CryptographyError e) {
                Assert.fail("could not create test case " + caseNo);
            }
        }
    }

    @Test
    public void testInsufficientFunds() {
        MockCrypto crypto = new MockCrypto(2222);
        Simulator sim = new Simulator(new MockMessageFactory(), crypto);
        int caseNo = 0;

        // Tests for players who initially have insufficient funds.
        InsufficientFunds(caseNo++, 2, new int[]{1}, new int[]{}, new int[]{}, sim).check();
        InsufficientFunds(caseNo++, 2, new int[]{}, new int[]{1}, new int[]{}, sim).check();
        InsufficientFunds(caseNo++, 2, new int[]{}, new int[]{}, new int[]{1}, sim).check();
        InsufficientFunds(caseNo++, 2, new int[]{1, 2}, new int[]{}, new int[]{}, sim).check();
        InsufficientFunds(caseNo++, 3, new int[]{1}, new int[]{}, new int[]{}, sim).check();
        InsufficientFunds(caseNo++, 5, new int[]{3}, new int[]{}, new int[]{}, sim).check();
        InsufficientFunds(caseNo++, 5, new int[]{}, new int[]{4}, new int[]{}, sim).check();
        InsufficientFunds(caseNo++, 5, new int[]{}, new int[]{}, new int[]{5}, sim).check();
        InsufficientFunds(caseNo++, 10, new int[]{5, 10}, new int[]{}, new int[]{}, sim).check();
        InsufficientFunds(caseNo++, 10, new int[]{}, new int[]{1, 2}, new int[]{}, sim).check();
        InsufficientFunds(caseNo++, 10, new int[]{}, new int[]{}, new int[]{3, 5}, sim).check();
        InsufficientFunds(caseNo++, 10, new int[]{5}, new int[]{10}, new int[]{}, sim).check();
        InsufficientFunds(caseNo++, 10, new int[]{}, new int[]{3}, new int[]{9}, sim).check();
        InsufficientFunds(caseNo,   10, new int[]{1}, new int[]{}, new int[]{2}, sim).check();
    }

    @Test
    public void testShuffleMachine() {
        MockCrypto crypto = new MockCrypto(2223);
        Simulator sim = new Simulator(new MockMessageFactory(), crypto);
        int caseNo = 0;

        // Tests for players who spend funds while the protocol is going on.
        DoubleSpend(caseNo++, new int[]{0, 0}, new int[]{1}, sim).check();
        DoubleSpend(caseNo++, new int[]{0, 1, 0}, new int[]{1}, sim).check();
        DoubleSpend(caseNo++, new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1}, new int[]{6}, sim).check();
        DoubleSpend(caseNo++, new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1}, new int[]{3, 10}, sim).check();
        DoubleSpend(caseNo++, new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1}, new int[]{1, 7, 8}, sim).check();
        DoubleSpend(caseNo, new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1}, new int[]{4, 6, 7, 8}, sim).check();

    }

    private class Equivocation {
        final int equivocator;
        final int[] equivocation;


        private Equivocation(int equivocator, int[] equivocation) {
            this.equivocator = equivocator;
            this.equivocation = equivocation;
        }
    }

    @Test
    public void testEquivocation() {
        MockCrypto crypto = new MockCrypto(87);
        Simulator sim = new Simulator(new MockMessageFactory(), crypto);
        int caseNo = 0;

        // A player sends different encryption keys to different players.
        EquivocateAnnouncement(caseNo++, 3,
                new Equivocation[]{
                        new Equivocation(1, new int[]{2})}, sim).check();
        EquivocateAnnouncement(caseNo++, 10,
                new Equivocation[]{
                        new Equivocation(1, new int[]{4, 5})}, sim).check();
        EquivocateAnnouncement(caseNo++, 10,
                new Equivocation[]{
                        new Equivocation(5, new int[]{1, 10}),
                        new Equivocation(7, new int[]{2, 8})}, sim).check();
        EquivocateAnnouncement(caseNo++, 10,
                new Equivocation[]{
                        new Equivocation(2, new int[]{3}),
                        new Equivocation(4, new int[]{5, 6}),
                        new Equivocation(8, new int[]{9})}, sim).check();
        // A player sends different output vectors to different players.
        EquivocateOutput(caseNo++, 3,  new int[]{1}, sim);
        EquivocateOutput(caseNo++, 3,  new int[]{2}, sim);
        EquivocateOutput(caseNo++, 4,  new int[]{1}, sim);
        EquivocateOutput(caseNo++, 4,  new int[]{1, 2}, sim);
        EquivocateOutput(caseNo  , 10, new int[]{3, 5, 7}, sim);
    }

    @Test
    public void testShuffleMalice() {

        // A player drops an address during phase 2.
        // A player drops an address and adds another one in phase 2.
        // A player drops an address and adds a duplicate in phase 2.
    }

    @Test
    public void testTransactionDisagreement() {
        // Player generates a different transaction signature to everyone else.
    }

    @Test
    public void testLies() {

        // A player lies about the equivocation check.
        // A player claims something went wrong in phase 2 when it didn't.
    }

    // Error test cases that I need to make:

    // A player disconnects at the wrong time.
    // Different combinations of these at the same time.
}
