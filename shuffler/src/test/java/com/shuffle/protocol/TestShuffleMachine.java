package com.shuffle.protocol;

import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.mock.MockCrypto;
import com.shuffle.mock.MockMessageFactory;
import com.shuffle.mock.MockSessionIdentifier;
import com.shuffle.sim.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * Integration tests for the protocol.
 *
 * Created by Daniel Krawisz on 12/10/15.
 */
public class TestShuffleMachine {
    private static Logger log = LogManager.getLogger(TestShuffleMachine.class);

    int seed = 99;

    public class MockTestCase extends TestCase {

        protected MockTestCase(String description) {
            super(17, new MockMessageFactory(), new MockSessionIdentifier(description));
        }

        @Override
        protected Crypto crypto() {
            return new MockCrypto(++seed);
        }
    }

    public static void check(String description, InitialState init) {
        Assert.assertTrue("failure in test " + description, com.shuffle.sim.TestCase.test(init, new MockMessageFactory()).isEmpty());
    }

    // Create a test case representing a successful run.
    public void SuccessfulRun(int caseNo, int numPlayer) {
        String description = "case " + caseNo + "; successful run with " + numPlayer + " players.";
        check(description, new MockTestCase(description).successfulTestCase(numPlayer));
    }
    
    public void InsufficientFunds(
            int caseNo,
            int numPlayers,
            int[] deadbeats,
            int[] poor,
            int[] spenders) {
        String description = "case " + caseNo + "; Insufficient funds test case.";
        check(description, new MockTestCase(description).insufficientFundsTestCase(numPlayers, deadbeats, poor, spenders));
    }

    public void DoubleSpend(int caseNo, int[] views, int[] doubleSpenders) {
        String description = "case " + caseNo + "; Double spend test case.";
        check(description, new MockTestCase(description).doubleSpendTestCase(views, doubleSpenders));
    }

    // Run a test case for equivocation during phase 1.
    public void EquivocateAnnouncement(
            int caseNo,
            int numPlayers,
            InitialState.Equivocation[] equivocators
    ) {
        String description = "case " + caseNo + "; announcement equivocation test case.";
        check(description, new MockTestCase(description).equivocateAnnouncementTestCase(numPlayers, equivocators));
    }

    // Run a test case for equivocation during phase 3.
    public void EquivocateOutput(int caseNo, int numPlayers, int[] equivocation) {
        String description = "case " + caseNo + "; broadcast equivocation test case.";
        check(description, new MockTestCase(description).equivocateBroadcastTestCase(numPlayers, equivocation));
    }

    // Run a test case for a player who drops an address in phase 2.
    public void DropAddress(int caseNo, int numPlayers, int[][] drop, int[][] replaceNew, int[][] replaceDuplicate) {
        String description = "case " + caseNo + "; shuffle phase mischief test case.";
        check(description, new MockTestCase(description).dropAddressTestCase(numPlayers, drop, replaceNew, replaceDuplicate));
    }

    public void InvalidTransactionSignature(int caseNo, int numPlayers, int[] mutants) {
        String description = "case " + caseNo + "; invalid transaction signature test case.";
        check(description, new MockTestCase(description).invalidSignatureTestCase(numPlayers, mutants));
    }

    @Test
    // Tests for successful runs of the protocol.
    public void testSuccess() {

        // Tests for successful runs.
        int caseNo = 0;
        int minPlayers = 2;
        int maxPlayers = 12;
        for (int numPlayer = minPlayers; numPlayer <= maxPlayers; numPlayer++) {
            try {
                SuccessfulRun(caseNo, numPlayer);
                caseNo++;
            } catch (CryptographyError e) {
                Assert.fail("could not create test case " + caseNo);
            }
        }
    }

    @Test
    // Tests for players who come in without enough cash.
    public void testInsufficientFunds() {
        int caseNo = 0;

        // Tests for players who initially have insufficient funds.
        InsufficientFunds(caseNo++, 2, new int[]{1}, new int[]{}, new int[]{});
        InsufficientFunds(caseNo++, 2, new int[]{}, new int[]{1}, new int[]{});
        InsufficientFunds(caseNo++, 2, new int[]{}, new int[]{}, new int[]{1});
        InsufficientFunds(caseNo++, 2, new int[]{1, 2}, new int[]{}, new int[]{});
        InsufficientFunds(caseNo++, 3, new int[]{1}, new int[]{}, new int[]{});
        InsufficientFunds(caseNo++, 5, new int[]{3}, new int[]{}, new int[]{});
        InsufficientFunds(caseNo++, 5, new int[]{}, new int[]{4}, new int[]{});
        InsufficientFunds(caseNo++, 5, new int[]{}, new int[]{}, new int[]{5});
        InsufficientFunds(caseNo++, 10, new int[]{5, 10}, new int[]{}, new int[]{});
        InsufficientFunds(caseNo++, 10, new int[]{}, new int[]{1, 2}, new int[]{});
        InsufficientFunds(caseNo++, 10, new int[]{}, new int[]{}, new int[]{3, 5});
        InsufficientFunds(caseNo++, 10, new int[]{5}, new int[]{10}, new int[]{});
        InsufficientFunds(caseNo++, 10, new int[]{}, new int[]{3}, new int[]{9});
        InsufficientFunds(caseNo, 10, new int[]{1}, new int[]{}, new int[]{2});
    }

    @Test
    // Tests for malicious players who send different output vectors to different players.
    public void testEquivocationBroadcast() {
        int caseNo = 0;

        // A player sends different output vectors to different players.
        EquivocateOutput(caseNo++, 3, new int[]{1});
        EquivocateOutput(caseNo++, 3, new int[]{2});
        EquivocateOutput(caseNo++, 4, new int[]{1});
        EquivocateOutput(caseNo++, 4, new int[]{1, 2});
        EquivocateOutput(caseNo, 10, new int[]{3, 5, 7});
    }

    @Test
    public void testEquivocationAnnounce() {
        int caseNo = 0;

        // A player sends different encryption keys to different players.
        EquivocateAnnouncement(caseNo++, 3,
                new InitialState.Equivocation[]{
                        new InitialState.Equivocation(2, new int[]{3})});
        EquivocateAnnouncement(caseNo++, 5,
                new InitialState.Equivocation[]{
                        new InitialState.Equivocation(2, new int[]{4, 5})});
        EquivocateAnnouncement(caseNo++, 10,
                new InitialState.Equivocation[]{
                        new InitialState.Equivocation(2, new int[]{4, 10}),
                        new InitialState.Equivocation(5, new int[]{7, 8})});
        EquivocateAnnouncement(caseNo, 10,
                new InitialState.Equivocation[]{
                        new InitialState.Equivocation(2, new int[]{3}),
                        new InitialState.Equivocation(4, new int[]{5, 6}),
                        new InitialState.Equivocation(8, new int[]{9})});
    }

    @Test
    // Tests for failures during the shuffle phase.
    public void testShuffleMalice() {
        int caseNo = 0;

        // A player drops an address during phase 2.
        DropAddress(caseNo++, 3, new int[][]{new int[]{2, 1}}, null, null);
        DropAddress(caseNo++, 3, new int[][]{new int[]{3, 2}}, null, null);
        DropAddress(caseNo++, 4, new int[][]{new int[]{3, 2}}, null, null);

        // A player drops an address and adds another one in phase 2.
        DropAddress(caseNo++, 3, null, new int[][]{new int[]{2, 1}}, null);
        DropAddress(caseNo++, 3, null, new int[][]{new int[]{3, 2}}, null);
        DropAddress(caseNo++, 4, null, new int[][]{new int[]{3, 2}}, null);

        // A player drops an address and adds a duplicate in phase 2.
        DropAddress(caseNo++, 4, null, null, new int[][]{new int[]{3, 1, 2}});
        DropAddress(caseNo++, 4, null, null, new int[][]{new int[]{4, 3, 2}});
        DropAddress(caseNo, 5, null, null, new int[][]{new int[]{4, 3, 2}});
    }

    // TODO make these work.
    @Test
    public void testTransactionDisagreement() {
        int caseNo = 0;

        // Player generates a different transaction signature to everyone else.
        InvalidTransactionSignature(caseNo++, 2, new int[]{2});
        InvalidTransactionSignature(caseNo++, 5, new int[]{2});
        InvalidTransactionSignature(caseNo++, 5, new int[]{2, 3});
        InvalidTransactionSignature(caseNo, 10, new int[]{2, 5, 6, 7});
    }

    @Test
    public void testDoubleSpending() {
        int caseNo = 0;

        // Tests for players who spend funds while the protocol is going on.
        DoubleSpend(caseNo++, new int[]{0, 0}, new int[]{1});
        DoubleSpend(caseNo++, new int[]{0, 1, 0}, new int[]{1});
        DoubleSpend(caseNo++, new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1}, new int[]{6});
        DoubleSpend(caseNo++, new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1}, new int[]{3, 10});
        DoubleSpend(caseNo, new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1}, new int[]{1, 7});

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
