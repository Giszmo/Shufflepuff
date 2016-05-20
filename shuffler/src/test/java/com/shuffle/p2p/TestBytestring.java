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

        Assert.assertTrue(e.append(a).equals(a));
        Assert.assertTrue(e.prepend(a).equals(a));
        Assert.assertTrue(e.append(b).equals(b));
        Assert.assertTrue(e.prepend(b).equals(b));
        Assert.assertTrue(a.append(e).equals(a));
        Assert.assertTrue(a.prepend(e).equals(a));
        Assert.assertTrue(b.append(e).equals(b));
        Assert.assertTrue(b.prepend(e).equals(b));

        Assert.assertTrue(a.append(b).equals(ab));
        Assert.assertTrue(a.prepend(b).equals(ba));
        Assert.assertTrue(b.append(a).equals(ba));
        Assert.assertTrue(b.prepend(a).equals(ab));
    }

    static int chopTestNum = 0;
    static void chopTestCase(byte[] input, int[] chop, byte[][] ex) {
        chopTestNum ++;
        Bytestring[] expected = new Bytestring[ex.length];
        int i = 0;
        for (byte[] b : ex) {
            expected[i] = new Bytestring(b);
            i++;
        }

        Bytestring[] results = new Bytestring(input).chop(chop);
        System.out.println("Chop test case " + chopTestNum
                + "; expected " + Arrays.toString(expected)
                + "; result " + Arrays.toString(results));

        Assert.assertTrue(Arrays.equals(expected, results));
    }

    @Test
    public void testChop() {
        chopTestCase(new byte[]{}, new int[]{}, new byte[][]{new byte[]{}});
        chopTestCase(new byte[]{}, new int[]{1}, new byte[][]{new byte[]{}, new byte[]{}});
        chopTestCase(new byte[]{}, new int[]{1, 3},
                new byte[][]{new byte[]{}, new byte[]{}, new byte[]{}});
        chopTestCase(new byte[]{1}, new int[]{}, new byte[][]{new byte[]{1}});
        chopTestCase(new byte[]{1}, new int[]{0}, new byte[][]{new byte[]{}, new byte[]{1}});
        chopTestCase(new byte[]{1}, new int[]{1}, new byte[][]{new byte[]{1}, new byte[]{}});
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
}
