package com.shuffle.protocol;

import com.shuffle.cryptocoin.Address;
import com.shuffle.cryptocoin.Coin;
import com.shuffle.cryptocoin.CryptographyError;
import com.shuffle.cryptocoin.SigningKey;
import com.shuffle.cryptocoin.Transaction;
import com.shuffle.cryptocoin.VerificationKey;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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

    BlameMatrixPatternAny anyMatrix = new BlameMatrixPatternAny();
    BlameEvidencePatternAny anyReason = new BlameEvidencePatternAny();

    public class TestCase {

        String description = null;
        SessionIdentifier session;
        long amount;
        Map<SigningKey, ReturnState> expected = new HashMap<>();
        Map<SigningKey, ReturnState> results = new HashMap<>();

        TestCase(SessionIdentifier session, long amount, String desc) {
            this.session = session;
            this.description = desc;
            this.amount = amount;
        }

        TestCase put(SigningKey key, ReturnState ex, ReturnState r) {
            results.put(key, r);
            expected.put(key, ex);
            return this;
        }

        public void putSuccessfulPlayer(SigningKey key, ReturnState r) {
            put(key, new ReturnState(true, session, Phase.Completed, null, null), r);
        }
    }

    TestCase successfulExpectation(TestCase test, Map<SigningKey, ReturnState> results) {
        for (SigningKey key : results.keySet()) {
            test.putSuccessfulPlayer(key, results.get(key));
        }
        return test;
    }

    // Create a test case representing a successful run.
    public TestCase SuccessfulRun(int numPlayer, Simulator sim) {
        SessionIdentifier mockSessionIdentifier = new MockSessionIdentifier();
        MockCoin coin = new MockCoin();
        long amount = 17;

        return successfulExpectation(
                new TestCase(mockSessionIdentifier, amount, "successful run with " + numPlayer + " players."),
                sim.successfulRun(mockSessionIdentifier, numPlayer, amount, coin)
        );
    }
    
    public TestCase InsufficientFunds(
            int numPlayers,
            int[] deadbeats,
            int[] poor,
            int[] spenders,
            Simulator sim) {
        SessionIdentifier mockSessionIdentifier = new MockSessionIdentifier();
        MockCoin coin = new MockCoin();
        long amount = 17;
        TestCase test = new TestCase(mockSessionIdentifier, amount, "Insufficient funds test case.");

        Map<SigningKey, ReturnState> results =
                sim.insufficientFundsRun(mockSessionIdentifier, numPlayers, deadbeats, poor, spenders, amount, coin);

        // If no offenders were defined, then this should be a successful run.
        if (deadbeats.length == 0 && poor.length == 0 && spenders.length == 0) {
            return successfulExpectation(test, results);
        }

        // The set of offending transactions.
        Map<SigningKey, Transaction> offenders = new HashMap<>();
        Set<SigningKey> deadbeatPlayers = new HashSet<>();

        // Get transactions by key.
        for (SigningKey key : results.keySet()) {
            BlameMatrix bm = null;

            Transaction t = coin.getOffendingTransaction(key.VerificationKey().address(), 17);

            if (t != null) {
                offenders.put(key, t);
            }

            if (coin.valueHeld(key.VerificationKey().address()) == 0) {
                deadbeatPlayers.add(key);
            }
        }

        for (SigningKey i : results.keySet()) {
            BlameMatrix bm = null;

            if (offenders.containsKey(i) || deadbeatPlayers.contains(i)) {
                bm = anyMatrix;
            } else {

                bm = new BlameMatrix();

                for (SigningKey j : results.keySet()) {
                    for (SigningKey k : results.keySet()) {
                        if (offenders.containsKey(i) || deadbeatPlayers.contains(i)) {
                            // We don't care who the malicious players accuse.
                            bm.add(j.VerificationKey(), k.VerificationKey(), anyReason);
                        } else if(deadbeatPlayers.contains(k)) {
                            bm.add(j.VerificationKey(), k.VerificationKey(),
                                    new BlameMatrix.BlameEvidence(BlameMatrix.BlameReason.NoFundsAtAll, true));
                        } else if(offenders.containsKey(k)) {
                            bm.add(j.VerificationKey(), k.VerificationKey(),
                                    new BlameMatrix.BlameEvidence(BlameMatrix.BlameReason.InsufficientFunds, true, offenders.get(k)));
                        }
                    }
                }
            }

            test.put(i,
                    new ReturnState(true, mockSessionIdentifier, Phase.Completed, null, bm),
                    results.get(i));
        }
        
        return test;
    }
    
    public TestCase DoubleSpend(int[] views, int[] doubleSpenders, Simulator sim) {

        SessionIdentifier mockSessionIdentifier = new MockSessionIdentifier();
        long amount = 17;
        TestCase test = new TestCase(mockSessionIdentifier, amount, "Double spending test case.");

        Map<Integer, Simulator.MockCoin> coinNets = new HashMap<>();

        for (int i = 0; i < views.length; i ++) {
            if (!coinNets.containsKey(i)) {
                coinNets.put(i, new MockCoin());
            }
        }

        LinkedHashMap<SigningKey, ReturnState> results =
                sim.doubleSpendingRun(mockSessionIdentifier, views, coinNets, doubleSpenders, amount);

        // The set of offending transactions.
        Map<SigningKey, Transaction> offenders = new HashMap<>();
        Map<SigningKey, Simulator.MockCoin> playerToCoin = new HashMap<>();

        {int i = 0;
        for (SigningKey key : results.keySet()) {

            BlameMatrix bm = null;
            Simulator.MockCoin coin = coinNets.get(views[i]);
            playerToCoin.put(key, coin);

            Transaction t = coin.getOffendingTransaction(key.VerificationKey().address(), 17);

            if (t != null) {
                offenders.put(key, t);
            }

            i++;
        }}

        for (SigningKey i : results.keySet()) {
            BlameMatrix bm = null;

            if (offenders.containsKey(i)) {
                bm = anyMatrix;

            } else {
                bm = new BlameMatrix();
                for (SigningKey j : results.keySet()) {
                    for (SigningKey k : results.keySet()) {

                        if (offenders.containsKey(j)) {
                            // We don't care who the malicious players accuse.
                            bm.add(j.VerificationKey(), k.VerificationKey(), anyReason);
                        } else if(offenders.containsKey(k)) {
                            if (playerToCoin.get(j) == playerToCoin.get(k)) {
                                // Only include the transaction if the player has the same view
                                // of the network as the double spender.
                                bm.add(j.VerificationKey(),k.VerificationKey(),
                                        new BlameMatrix.BlameEvidence(BlameMatrix.BlameReason.DoubleSpend, true, offenders.get(k)));
                            }
                        }
                    }
                }
            }

            test.put(i,
                    new ReturnState(true, mockSessionIdentifier, Phase.Completed, null, bm),
                    results.get(i));
        }

        return test;
    }

    @Test
    // TODO separate the different kinds of tests into different functions.
    public void testShuffleMachine() {
        Map<Integer, TestCase> tests = new HashMap<>();
        MockCrypto crypto = new MockCrypto(2222);
        Simulator sim = new Simulator(new MockMessageFactory(), crypto);

        // Tests for successful runs.
        int caseNo = 0;
        for (int numPlayer = 2; numPlayer <= 12; numPlayer ++ ) {
            try {
                tests.put(caseNo, SuccessfulRun(numPlayer, sim));
                caseNo++;
            } catch (CryptographyError e) {
                Assert.fail("could not create test case " + caseNo);
            }
        }

        // Tests for players who initially have insufficient funds.
        TestCase[] insufficientFundsCases = new TestCase[]{
                InsufficientFunds(10, new int[]{3}, new int[]{}, new int[]{}, sim),
                InsufficientFunds(10, new int[]{}, new int[]{4}, new int[]{}, sim),
                InsufficientFunds(10, new int[]{}, new int[]{}, new int[]{5}, sim),
                InsufficientFunds(10, new int[]{5, 10}, new int[]{}, new int[]{}, sim),
                InsufficientFunds(10, new int[]{}, new int[]{1, 2}, new int[]{}, sim),
                InsufficientFunds(10, new int[]{}, new int[]{}, new int[]{3, 5}, sim),
                InsufficientFunds(10, new int[]{5}, new int[]{10}, new int[]{}, sim),
                InsufficientFunds(10, new int[]{}, new int[]{3}, new int[]{9}, sim),
                InsufficientFunds(10, new int[]{1}, new int[]{}, new int[]{2}, sim),
        };

        // Tests for players who initially have insufficient funds.
        TestCase[] doubleSpendCases = new TestCase[]{
                DoubleSpend(new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1}, new int[]{6}, sim),
                DoubleSpend(new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1}, new int[]{3, 10}, sim),
                DoubleSpend(new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1}, new int[]{1, 7, 8}, sim),
                DoubleSpend(new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1}, new int[]{4, 6, 7, 8}, sim)
        };

        // Set up tests for players with insufficient funds.
        for (TestCase maliciousCase : insufficientFundsCases) {
            tests.put(caseNo, maliciousCase);
            caseNo ++;
        }

        // Set up tests for players who double spend.
        for (TestCase maliciousCase : doubleSpendCases) {
            tests.put(caseNo, maliciousCase);
            caseNo ++;
        }

        Map<Phase, Map<VerificationKey, Packet>>[] messageReplacements;

        // Error test cases that I need to make:
        // A player sends different encryption keys to different players.
        // A player sends different output vectors to different players.
        // A player drops an address during phase 2.
        // A player drops an address and adds another one in phase 2.
        // A player drops an address and adds a duplicate in phase 2.

        // A player lies about the equivocation check.
        // A player claims something went wrong in phase 2 when it didn't.
        // Player lies about what another player said (invalid signature).
        // A player disconnects at the wrong time.
        // Different combinations of these at the same time.

        // Run the tests.
        for (Map.Entry<Integer, TestCase> testEntry : tests.entrySet()) {
            int testNo = testEntry.getKey();
            TestCase test = testEntry.getValue();
            System.out.println("running test case " + testNo  + (test.description != null ? ": " + test.description : ""));

            if(test.amount == 0){
                Assert.fail();
            }

            Map<SigningKey, ReturnState> results = test.results;

            // Check that the map of error states returned matches that which was expected.
            for (Map.Entry<SigningKey, ReturnState> machine : test.expected.entrySet()) {
                SigningKey key = machine.getKey();
                ReturnState result = results.get(key);
                ReturnState expected = machine.getValue();

                Assert.assertNotNull(result);

                if (expected.success && !result.success) {
                    if (result.error != null) {
                        result.error.printStackTrace();
                    }
                    if (result.blame != null) {
                        System.out.println(result.blame.toString());
                    }
                }

                System.out.println("  result " + key.toString() + " : " + result.toString());
                System.out.println("  expected " + key.toString() + " : " + expected.toString());
                Assert.assertTrue(expected.match(result));

                results.remove(key);
            }

            // I don't know why there would be any left, but just to make sure!
            Assert.assertTrue(results.isEmpty());
        }
    }
}
