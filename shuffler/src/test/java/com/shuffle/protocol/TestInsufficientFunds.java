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
 * Tests for players with insufficient funds.
 *
 * Created by Daniel Krawisz on 3/17/16.
 */
public class TestInsufficientFunds extends TestShuffleMachine{

    private void InsufficientFunds(
            int numPlayers,
            int[] deadbeats,
            int[] poor,
            int[] spenders) {
        String description = "case " + caseNo + "; Insufficient funds test case.";
        check(new MockTestCase(description).insufficientFundsTestCase(
                numPlayers, deadbeats, poor, spenders
        ));
    }

    @Test
    // Tests for players who come in without enough cash.
    public void testInsufficientFunds() {
        // Tests for players who initially have insufficient funds.
        InsufficientFunds(2, new int[]{1}, new int[]{}, new int[]{});
        InsufficientFunds(2, new int[]{}, new int[]{1}, new int[]{});
        InsufficientFunds(2, new int[]{}, new int[]{}, new int[]{1});
        InsufficientFunds(2, new int[]{1, 2}, new int[]{}, new int[]{});
        InsufficientFunds(3, new int[]{1}, new int[]{}, new int[]{});
        InsufficientFunds(5, new int[]{3}, new int[]{}, new int[]{});
        InsufficientFunds(5, new int[]{}, new int[]{4}, new int[]{});
        InsufficientFunds(5, new int[]{}, new int[]{}, new int[]{5});
        InsufficientFunds(10, new int[]{5, 10}, new int[]{}, new int[]{});
        InsufficientFunds(10, new int[]{}, new int[]{1, 2}, new int[]{});
        InsufficientFunds(10, new int[]{}, new int[]{}, new int[]{3, 5});
        InsufficientFunds(10, new int[]{5}, new int[]{10}, new int[]{});
        InsufficientFunds(10, new int[]{}, new int[]{3}, new int[]{9});
        InsufficientFunds(10, new int[]{1}, new int[]{}, new int[]{2});
    }
}
