package com.shuffle.p2p;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * Created by Daniel Krawisz on 5/21/16.
 */
public class TestBytestring {

    @Test
    public void testAppendPrepend() {
        Bytestring e = new Bytestring(new byte[]{});
        Bytestring a = new Bytestring(new byte[]{2, 3, 7});
        Bytestring b = new Bytestring(new byte[]{12, 13});

        Bytestring ab = new Bytestring(new byte[]{2, 3, 7, 12, 13});
        Bytestring ba = new Bytestring(new byte[]{12, 13, 2, 3, 7});

        Bytestring[][] values = new Bytestring[][]{
                {e, e, e},
                {e, a, a},
                {e, b, b},
                {a, e, a},
                {b, e, b},
                {a, b, ab},
                {b, a, ba}
        };

        for(Bytestring[] v: values) {
            Assert.assertEquals(v[0].append(v[1]), v[2]);
            Assert.assertEquals(v[1].prepend(v[0]), v[2]);
        }
    }

    void chopTestCase(byte[] input, int[] chop, byte[][] expected) {
        Bytestring[] results = new Bytestring(input).chop(chop);
        byte[][] resultBAs = new byte[results.length][];
        for(int i=0; i<results.length; i++) {
            resultBAs[i] = results[i].bytes;
        }
        String msg = "Chop test case; expected " + Arrays.deepToString(expected)
                + "; result " + Arrays.deepToString(resultBAs);

        Assert.assertTrue(msg, Arrays.deepEquals(expected, resultBAs));
    }

    @Test
    public void testChop() {
        chopTestCase(new byte[]{}, new int[]{}, new byte[][]{});
        chopTestCase(new byte[]{1}, new int[]{}, new byte[][]{new byte[]{1}});
        chopTestCase(
                new byte[]{1, 2},
                new int[]{1},
                new byte[][]{new byte[]{1}, new byte[]{2}});
        chopTestCase(
                new byte[]{1, 2, 3},
                new int[]{1, 2},
                new byte[][]{new byte[]{1}, new byte[]{2}, new byte[]{3}});
        chopTestCase(
                new byte[]{1, 2, 3},
                new int[]{1},
                new byte[][]{new byte[]{1}, new byte[]{2, 3}});
        chopTestCase(
                new byte[]{1, 2, 3},
                new int[]{2},
                new byte[][]{new byte[]{1, 2}, new byte[]{3}});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testChopFailsChoppingBeyondEmptyArray() {
        chopTestCase(new byte[]{}, new int[]{1}, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testChopFailsChoppingAtZero() {
        chopTestCase(new byte[]{1}, new int[]{0}, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testChopFailsChoppingAtLength() {
        chopTestCase(new byte[]{1, 3, 4}, new int[]{3}, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testChopFailsChoppingOutOfOrder() {
        chopTestCase(new byte[]{1, 3, 4}, new int[]{2, 1}, null);
    }
}
