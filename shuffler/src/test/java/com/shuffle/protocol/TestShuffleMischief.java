/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

import org.junit.Test;


/**
 * During the shuffle phase, a player can do various things wrong. Elaborate checks are required
 * to detect and identify the mischief-maker.
 *
 * Created by Daniel Krawisz on 3/17/16.
 */
public class TestShuffleMischief extends TestShuffleMachine {

    public TestShuffleMischief() {
        super(99, 10);
    }

    // Run a test case for a player who drops an address in phase 2.
    private void DropAddress(
            int numPlayers,
            int[][] drop,
            int[][] replaceNew,
            int[][] replaceDuplicate
    ) {
        String description = "case " + caseNo + "; shuffle phase mischief test case.";
        check(new NoShuffleTestCase(description).dropAddressTestCase(
                numPlayers, drop, replaceNew, replaceDuplicate
        ));
    }

    @Test
    public void testDropAddress() {
        DropAddress(2, new int[][]{new int[]{1, 1}}, null, null);
        DropAddress(2, new int[][]{new int[]{2, 1}}, null, null);
        DropAddress(2, new int[][]{new int[]{2, 2}}, null, null);
        DropAddress(3, new int[][]{new int[]{2, 1}}, null, null);
        DropAddress(3, new int[][]{new int[]{3, 2}}, null, null);
        DropAddress(3, new int[][]{new int[]{3, 3}}, null, null);
        DropAddress(3, new int[][]{new int[]{3, 1}}, null, null);
        DropAddress(4, new int[][]{new int[]{3, 2}}, null, null);
        DropAddress(4, new int[][]{new int[]{4, 1}}, null, null);
        DropAddress(4, new int[][]{new int[]{4, 2}}, null, null);
    }

    @Test
    public void testDropAddressReplaceNew() {
        DropAddress(3, null, new int[][]{new int[]{2, 1}}, null);
        DropAddress(3, null, new int[][]{new int[]{3, 2}}, null);
        DropAddress(4, null, new int[][]{new int[]{3, 2}}, null);
    }

    @Test
    // Tests for failures during the shuffle phase.
    public void testDropAddressDuplicate() {

        // A player drops an address and adds a duplicate in phase 2.
        DropAddress(4, null, null, new int[][]{new int[]{3, 1, 2}});
        DropAddress(4, null, null, new int[][]{new int[]{4, 3, 2}});
        DropAddress(5, null, null, new int[][]{new int[]{4, 3, 2}});

    }
}
