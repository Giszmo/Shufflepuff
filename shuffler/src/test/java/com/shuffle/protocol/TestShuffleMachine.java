package com.shuffle.protocol;

import com.shuffle.cryptocoin.Address;
import com.shuffle.cryptocoin.SigningKey;
import com.shuffle.cryptocoin.Transaction;
import com.shuffle.cryptocoin.VerificationKey;

import org.junit.Assert;
import org.junit.Test;

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

    public static class Expected {
        SigningKey identity;
        com.shuffle.protocol.Simulator.Adversary input;
        ReturnState output;

        public Expected(SigningKey identity, Simulator.Adversary input, ReturnState output) {
            this.identity = identity;
            this.input = input;
            this.output = output;
        }
    }

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
        Map<SigningKey, Expected> machines = new HashMap<>();

        TestCase(SessionIdentifier session, long amount, String desc) {
            this.session = session;
            this.description = desc;
            this.amount = amount;
        }

        TestCase put(SigningKey key, Expected ex) {
            machines.put(key, ex);
            return this;
        }
    }

    // Create a test case representing a successful run.
    public TestCase SuccessfulRun(int numPlayer, Simulator sim) {
        SessionIdentifier mockSessionIdentifier = new MockSessionIdentifier();
        MockCoin coin = new MockCoin();
        long amount = 17;
        TestCase test = new TestCase(mockSessionIdentifier, amount, "successful run with " + numPlayer + " players.");

        SortedSet<VerificationKey> players = new TreeSet<>();
        List<SigningKey> keys = new LinkedList<>();

        for (int i = 1; i <= numPlayer; i++) {
            SigningKey key = sim.crypto.SigningKey();
            players.add(key.VerificationKey());
            keys.add(key);
        }

        for (SigningKey key : keys) {
            Address address = key.VerificationKey().address();
            coin.put(address, 20);
            test.put(key,
                    new Expected(key,
                            sim.new HonestAdversary(mockSessionIdentifier, amount, key, players, coin),
                            new ReturnState(true, mockSessionIdentifier, Phase.Completed, null, null)
                    ));
        }
        return test;
    }
    
    public TestCase InsufficientFunds(
            int numPlayers,
            int[] deadbeats,
            int[] poor,
            int[] spenders,
            Simulator sim) {
        SortedSet<Integer> deadbeatSet = new TreeSet<>();
        SortedSet<Integer> spendersSet = new TreeSet<>();
        SortedSet<Integer> poorSet = new TreeSet<>();

        for(int i = 0; i < deadbeats.length; i++) {
            deadbeatSet.add(deadbeats[i]);
        }

        for(int i = 0; i < poor.length; i++) {
            if(!deadbeatSet.contains(poor[i])) {
                poorSet.add(poor[i]);
            }
        }

        for(int i = 0; i < spenders.length; i++) {
            if(!deadbeatSet.contains(spenders[i])
                    && !poorSet.contains(spenders[i])) {
                spendersSet.add(spenders[i]);
            }
        }

        Set<SigningKey> deadbeatKeys = new HashSet<>();
        Set<SigningKey> spendersKeys = new HashSet<>();
        Set<SigningKey> poorKeys = new HashSet<>();

        SortedSet<VerificationKey> players = new TreeSet<>();
        List<SigningKey> keys = new LinkedList<>();

        for (int i = 1; i <= numPlayers; i++) {
            SigningKey key = sim.crypto.SigningKey();
            players.add(key.VerificationKey());
            keys.add(key);

            if (deadbeatSet.contains(i)) {
                deadbeatKeys.add(key);
            } else if (poorSet.contains(i)) {
                poorKeys.add(key);
            } if (spendersSet.contains(i)) {
                spendersKeys.add(key);
            }
        }

        // The set of transactions in our fake bitcoin network.
        MockCoin coin = new MockCoin();

        // The transactions that are considered cheating.
        Map<SigningKey, Transaction> offenders = new HashMap<>();

        for (SigningKey key : keys) {
            Address previousAddress = sim.crypto.SigningKey().VerificationKey().address();

            // Each player starts with enough money.
            coin.put(previousAddress, 20);

            Address address = key.VerificationKey().address();

            if (poorKeys.contains(key)) {
                // Not enough money is in the poor person's address.
                offenders.put(key, coin.spend(previousAddress, address, 10));
            } else if (spendersKeys.contains(key)) {
                // We put enough money in the address.
                coin.spend(previousAddress, address, 20);

                // Plot twist! We spend it all!
                offenders.put(key, coin.spend(address, sim.crypto.SigningKey().VerificationKey().address(), 20));
            } else if (!deadbeatKeys.contains(key)) {
                // This is a normal, not cheating player. Deadbeat players
                // don't have anything, so we don't send anything to their address.
                coin.spend(previousAddress, address, 20);
            }

        }

        SessionIdentifier mockSessionIdentifier = new MockSessionIdentifier();
        long amount = 17;
        TestCase test = new TestCase(mockSessionIdentifier, amount, "Insufficient funds test case.");

        // Now we finally create the initial state.
        for (SigningKey i : keys) {

            BlameMatrix bm = null;
            boolean db = deadbeatKeys.contains(i), p = false, sp = false;

            if (!db) {
                p = poorKeys.contains(i);
                if (!p) {
                    sp = spendersKeys.contains(i);
                }
            }

            if (db || p || sp) {
                // malicious players don't have to return anything in particular.
                bm = anyMatrix;

            } else {
                bm = new BlameMatrix();

                for (SigningKey j : keys) {
                    for (SigningKey k : keys) {
                        if (deadbeatKeys.contains(j) || poorKeys.contains(j) || spendersKeys.contains(j)) {
                            // We don't care who the malicious players accuse.
                            bm.add(j.VerificationKey(), k.VerificationKey(), anyReason);
                        } else if(deadbeatKeys.contains(k)) {
                            bm.add(j.VerificationKey(), k.VerificationKey(),
                                    new BlameMatrix.BlameEvidence(BlameMatrix.BlameReason.NoFundsAtAll, true));
                        } else if(poorKeys.contains(k) || spendersKeys.contains(k)) {
                            bm.add(j.VerificationKey(), k.VerificationKey(),
                                    new BlameMatrix.BlameEvidence(BlameMatrix.BlameReason.InsufficientFunds, true, offenders.get(k)));
                        }
                    }
                }
            }

            test.put(i,
                    new Expected(i,
                            sim.new HonestAdversary(mockSessionIdentifier, amount, i, players, coin),
                            new ReturnState(false, mockSessionIdentifier, Phase.Blame, null, bm)
                    ));
        }
        
        return test;
    }
    
    public TestCase DoubleSpend(int[] views, int[] doubleSpenders, Simulator sim) {
        SortedSet<Integer> doubleSpendSet = new TreeSet<>();
        for(int i = 0; i < doubleSpenders.length; i++) {
            doubleSpendSet.add(doubleSpenders[i]);
        }

        Set<SigningKey> doubleSpendKeys = new HashSet<>();

        List<SigningKey> keys = new LinkedList<>();
        Map<Integer, MockCoin> coinNets = new HashMap<>();
        Map<SigningKey, MockCoin> playerToNetwork = new HashMap<>();
        Map<SigningKey, Transaction> doubleSpends = new HashMap<>();
        SortedSet<VerificationKey> players = new TreeSet<>();

        for(int i = 0; i < views.length; i ++) {
            // first find out how many networks we need.
            if (!coinNets.containsKey(i)) {
                coinNets.put(views[i], new MockCoin());
            }

            SigningKey key = sim.crypto.SigningKey();
            keys.add(key);
            playerToNetwork.put(key, coinNets.get(views[i]));

            if (doubleSpendSet.contains(i)) {
                doubleSpendKeys.add(key);
            }

            players.add(key.VerificationKey());
        }

        // Set up the networks with everyone having the correct initial amounts.
        for (SigningKey key : keys) {
            Address previousAddress = sim.crypto.SigningKey().VerificationKey().address();
            Address address = key.VerificationKey().address();

            for (MockCoin coin : coinNets.values()) {
                coin.put(previousAddress, 20);
                coin.spend(previousAddress, address, 20);
            }

            // Make double spend transaction if applicable.
            if (doubleSpendKeys.contains(key)) {
                List<MockCoin.Output> in = new LinkedList<>();
                List<MockCoin.Output> out = new LinkedList<>();
                in.add(new MockCoin.Output(address, 20));
                out.add(new MockCoin.Output(sim.crypto.SigningKey().VerificationKey().address(), 16));
                MockCoin.MockTransaction t = new MockCoin.MockTransaction(in, out);
            }

        }

        SessionIdentifier mockSessionIdentifier = new MockSessionIdentifier();
        long amount = 17;
        TestCase test = new TestCase(mockSessionIdentifier, amount, "Insufficient funds test case.");

        for (SigningKey i : keys) {

            if (doubleSpendKeys.contains(i)) {

                test.put(i,
                        new Expected(i,
                                sim.new MaliciousAdversary(mockSessionIdentifier, amount, i, players, playerToNetwork.get(i), new MockCrypto(2222),
                                        new HashMap<Phase, Map<VerificationKey, Packet>>(), doubleSpends.get(i)),
                                new ReturnState(true, mockSessionIdentifier, Phase.Blame, null, anyMatrix)
                        ));

            } else {
                BlameMatrix bm = new BlameMatrix();
                for (SigningKey j : keys) {
                    for (SigningKey k : keys) {

                        if (doubleSpendKeys.contains(j)) {
                            // We don't care who the malicious players accuse.
                            bm.add(j.VerificationKey(), k.VerificationKey(), anyReason);
                        } else if(doubleSpendKeys.contains(k)) {
                            if (playerToNetwork.get(j) == playerToNetwork.get(k)) {
                                // Only include the transaction if the player has the same view
                                // of the network as the double spender.
                                bm.add(j.VerificationKey(),k.VerificationKey(),
                                        new BlameMatrix.BlameEvidence(BlameMatrix.BlameReason.DoubleSpend, true, doubleSpends.get(k)));
                            }
                        }
                    }
                }

                test.put(i,
                        new Expected(i,
                                sim.new HonestAdversary(mockSessionIdentifier, amount, i, players, playerToNetwork.get(i)),
                                new ReturnState(true, mockSessionIdentifier, Phase.Blame, null, bm)
                        ));
            }

        }

        return null;
    }

    @Test
    public void testShuffleMachine() {
        Map<Integer, TestCase> tests = new HashMap<>();
        MockCrypto crypto = new MockCrypto(2222);
        Simulator sim = new Simulator(new MockMessageFactory(), crypto);

        // Tests for successful runs.
        int caseNo = 0;
        /*for (int numPlayer = 2; numPlayer <= 12; numPlayer ++ ) {
            try {
                tests.put(caseNo, SuccessfulRun(numPlayer, sim));
                caseNo++;
            } catch (CryptographyError e) {
                Assert.fail("could not create test case " + caseNo);
            }
        }*/

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
        /*for (TestCase maliciousCase : doubleSpendCases) {
            tests.put(caseNo, maliciousCase);
            caseNo ++;
        }*/

        Map<Phase, Map<VerificationKey, Packet>>[] messageReplacements;

        // Error test cases that I need to make:
        // A player sends different encryption keys to different players.
        // A player sends different output vectors to different players.
        // A player lies about the equivocation check.
        // A player drops an address during phase 2.
        // A player drops an address and adds another one in phase 2.
        // A player drops an address and adds a duplicate in phase 2.
        // A player claims something went wrong in phase 2 when it didn't.
        // Player lies about what another player said (invalid signature).
        // A player disconnects at the wrong time.
        // Different combinations of these at the same time.

        // Run the tests.
        for (Map.Entry<Integer, TestCase> testEntry : tests.entrySet()) {
            int testNo = testEntry.getKey();
            TestCase test = testEntry.getValue();
            System.out.println("running test case " + testNo  + (test.description != null ? ": " + test.description : ""));

            List<Simulator.Adversary> init = new LinkedList<>();
            for (Expected  machine : test.machines.values()) {
                init.add(machine.input);
            }

            if(test.amount == 0){
                Assert.fail();
            }
            Map<SigningKey, ReturnState> errors = sim.runSimulation(test.amount, init);

            // Check that the map of error states returned matches that which was expected.
            for (Map.Entry<SigningKey, Expected> machine : test.machines.entrySet()) {
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

                System.out.println("  result " + key.toString() + " : " + result.toString());
                System.out.println("  expected " + key.toString() + " : " + expected.output.toString());
                Assert.assertTrue(expected.output.match(result));

                errors.remove(key);
            }

            // I don't know why there would be any left, but just to make sure!
            Assert.assertTrue(errors.isEmpty());
        }
    }
}
