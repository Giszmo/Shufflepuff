/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

import com.shuffle.bitcoin.Crypto;
import com.shuffle.mock.AlwaysZero;
import com.shuffle.mock.InsecureRandom;
import com.shuffle.mock.MockCrypto;
import com.shuffle.mock.MockMessageFactory;
import com.shuffle.mock.MockSessionIdentifier;
import com.shuffle.sim.InitialState;
import com.shuffle.sim.TestCase;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

/**
 * During the shuffle phase, a player can do various things wrong. Elaborate checks are required
 * to detect and identify the mischief-maker.
 *
 * Created by Daniel Krawisz on 3/17/16.
 */
public class TestShuffleMischief extends TestShuffleMachine {
    int trials = 10;

    public class MockTestCase extends TestCase {

        protected MockTestCase(String description) {
            super(17, new MockMessageFactory(), new MockSessionIdentifier(description));
        }

        @Override
        protected Crypto crypto() {
            return new MockCrypto(new AlwaysZero());
        }
    }

    public class Report {
        final int trials;
        final int fail;
        final int success;

        public Report(int trials, int fail, int success) {
            this.trials = trials;
            this.fail = fail;
            this.success = success;
        }
    }

    List<Report> reports = new LinkedList<>();

    @Override
    public  void check(String description, InitialState init) {
        int fail = 0;
        int success = 0;

        for (int i = 0; i < trials; i ++ ) {
            if (i % 10 == 0) {
                System.out.println("Trial " + i + " in progress. ");
            }

            if (com.shuffle.sim.TestCase.test(init, new MockMessageFactory()).isEmpty() ) {
                success ++;
            } else {
                fail ++;
            }
        }

        System.out.println("of " + trials + " trials, " + success + " successes and " + fail + " failures. ");

        reports.add(new Report(trials, fail, success));
    }

    // Run a test case for a player who drops an address in phase 2.
    public void DropAddress(int numPlayers, int[][] drop, int[][] replaceNew, int[][] replaceDuplicate) {
        String description = "case " + caseNo + "; shuffle phase mischief test case.";
        caseNo++;
        check(description, new MockTestCase(description).dropAddressTestCase(numPlayers, drop, replaceNew, replaceDuplicate));
    }

    @After
    public void printReport() {

        int i = 1;
        boolean success = true;
        for (Report report : reports) {
            System.out.println("Result for test " + i);
            if (report.fail > 0) success = false;
            System.out.println("   Trials: " + report.trials + "; success: " + report.success + "; fail: " + report.fail);
            i ++;
        }

        Assert.assertTrue(success);
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
