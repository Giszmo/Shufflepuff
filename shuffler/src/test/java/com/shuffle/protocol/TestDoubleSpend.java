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
 * Tests for players who spend their funds while the protocol is going on.
 *
 * Created by Daniel Krawisz on 3/17/16.
 */
public class TestDoubleSpend extends TestShuffleMachine {
    public TestDoubleSpend() {
        super(99, 10);
    }

    private void DoubleSpend(int[] views, int[] doubleSpenders) {
        String description = "case " + caseNo + "; Double spend test case.";
        check(new MockTestCase(description).doubleSpendTestCase(views, doubleSpenders));
    }

    @Test
    public void testDoubleSpending() {
        // Tests for players who spend funds while
        // the protocol is going on.
        DoubleSpend(new int[]{0, 0}, new int[]{1});
        DoubleSpend(new int[]{0, 1, 0}, new int[]{1});
        DoubleSpend(new int[]{0, 1, 0}, new int[]{2});
        DoubleSpend(new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1}, new int[]{6});
        DoubleSpend(new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1}, new int[]{3, 10});
        DoubleSpend(new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1}, new int[]{1, 7});

    }
}
