/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

import com.shuffle.bitcoin.Crypto;
import com.shuffle.mock.InsecureRandom;
import com.shuffle.mock.MockCrypto;
import com.shuffle.mock.MockMessageFactory;
import com.shuffle.mock.MockSessionIdentifier;
import com.shuffle.sim.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the protocol.
 *
 * Created by Daniel Krawisz on 12/10/15.
 */
public class TestShuffleMachine {
    protected static Logger log = LogManager.getLogger(TestShuffleMachine.class);

    static int seed = 99;

    public int caseNo = 0;

    public class MockTestCase extends TestCase {

        protected MockTestCase(String description) {
            super(17, new MockMessageFactory(), new MockSessionIdentifier(description));
        }

        @Override
        protected Crypto crypto() {
            return new MockCrypto(new InsecureRandom(++seed));
        }
    }

    public void check(String description, InitialState init) {
        Assert.assertTrue("failure in test " + description, com.shuffle.sim.TestCase.test(init, new MockMessageFactory()).isEmpty());
    }

    @Before
    public void resetCaseNumber () {
        caseNo = 0;
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
