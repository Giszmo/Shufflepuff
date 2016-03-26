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
import com.shuffle.sim.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.util.LinkedList;
import java.util.List;

/**
 * Integration tests for the protocol.
 *
 * Created by Daniel Krawisz on 12/10/15.
 */
public class TestShuffleMachine {
    protected static Logger log = LogManager.getLogger(TestShuffleMachine.class);
    static int defaultSeed = 99;
    static int defaultTrials = 100;

    int seed = 99;

    public int caseNo = 0;

    int trials = 1;

    public TestShuffleMachine() {
        seed = defaultSeed;
        trials = defaultTrials;
    }

    public TestShuffleMachine(int seed, int trials) {
        this.seed = defaultSeed;
        this.trials = defaultTrials;
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

    public class MockTestCase extends TestCase {

        protected MockTestCase(String description) {
            super(17, new MockMessageFactory(), new MockSessionIdentifier(description));
        }

        @Override
        protected Crypto crypto() {
            return new MockCrypto(new InsecureRandom(++seed));
        }
    }

    public class NoShuffleTestCase extends TestCase {

        protected NoShuffleTestCase(String description) {
            super(17, new MockMessageFactory(), new MockSessionIdentifier(description));
        }

        @Override
        protected Crypto crypto() {
            ++seed;
            return new MockCrypto(new AlwaysZero());
        }
    }

    public  void check(String description, InitialState init) {
        int fail = 0;
        int success = 0;
        caseNo++;

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

    @Before
    public void resetCaseNumber () {
        caseNo = 0;
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

    /*@Test
    public void testLies() {

        // A player lies about the equivocation check.
        // A player claims something went wrong in phase 2 when it didn't.
        Assert.fail();
    }

    @Test
    // Players disconnect at different points during the protocol.
    // TODO must include cases in which a malicious player disconnects after sending a malicious message!!
    public void testDisconnect() {
        Assert.fail();
    }*/
}
