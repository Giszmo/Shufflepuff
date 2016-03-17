package com.shuffle.protocol;

import org.junit.Test;

/**
 * It is possible for a player to generate a different signature than everyone else. The players
 * all must check that the signature each of them generates matches the transaction that each of
 * them created.
 *
 * Created by Daniel Krawisz on 3/17/16.
 */
public class TestInvalidSignature extends TestShuffleMachine{

    public void InvalidTransactionSignature(int caseNo, int numPlayers, int[] mutants) {
        String description = "case " + caseNo + "; invalid transaction signature test case.";
        check(description, new MockTestCase(description).invalidSignatureTestCase(numPlayers, mutants));
    }

    @Test
    public void testInvalidSignature() {
        int caseNo = 0;

        // Player generates a different transaction signature to everyone else.
        InvalidTransactionSignature(caseNo++, 2, new int[]{2});
        InvalidTransactionSignature(caseNo++, 5, new int[]{2});
        InvalidTransactionSignature(caseNo++, 5, new int[]{2, 3});
        InvalidTransactionSignature(caseNo, 10, new int[]{2, 5, 6, 7});
    }
}
