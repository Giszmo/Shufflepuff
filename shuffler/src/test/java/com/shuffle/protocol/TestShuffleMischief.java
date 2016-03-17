package com.shuffle.protocol;

import org.junit.Test;

/**
 * During the shuffle phase, a player can do various things wrong. Elaborate checks are required
 * to detect and identify the mischief-maker.
 *
 * Created by Daniel Krawisz on 3/17/16.
 */
public class TestShuffleMischief extends TestShuffleMachine {

    // Run a test case for a player who drops an address in phase 2.
    public void DropAddress(int caseNo, int numPlayers, int[][] drop, int[][] replaceNew, int[][] replaceDuplicate) {
        String description = "case " + caseNo + "; shuffle phase mischief test case.";
        check(description, new MockTestCase(description).dropAddressTestCase(numPlayers, drop, replaceNew, replaceDuplicate));
    }

    @Test
    // Tests for failures during the shuffle phase.
    public void testShuffleMischief() {
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
}
