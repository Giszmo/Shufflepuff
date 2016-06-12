package com.shuffle.chan;


import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * Created by Daniel Krawisz on 5/20/16.
 */
public class TestInbox {

    static int testNum = 0;

    public static void runTest(int[] tpeers, int[] tmessages) throws InterruptedException {
        testNum++;
        System.out.println("Test case " + testNum);

        // The list of peers.
        final SortedSet<Integer> peers = new TreeSet<>();

        // Who should messages be sent from in this test?
        final List<Integer> messages = new LinkedList<>();

        final Inbox<Integer, Integer> inbox = new BasicInbox<>(100);
        final Map<Integer, Send<Integer>> send = new HashMap<>();
            
        for (int p : tpeers) {
            peers.add(p);
        }

        for (int m : tmessages) {
            messages.add(m);
        }

        // Create channels.
        for (Integer p : peers) {
            send.put(p, inbox.receivesFrom(p));
        }

        // Send messages.
        int msgnum = 0;
        for (Integer i : messages) {
            Send<Integer> v = send.get(i);

            if (v == null) return;

            Assert.assertTrue(v.send(msgnum));

            msgnum++;
        }

        // Close channels.
        for (Send<Integer> x : send.values()) {
            x.close();
            Assert.assertFalse(x.send(0));
        }

        inbox.close();

        // Channel should be closed.
        Assert.assertTrue(inbox.closed());

        // Drain messages
        for (Integer i : messages) {
            Inbox.Envelope<Integer, Integer> z = inbox.receive(100, TimeUnit.MILLISECONDS);

            Assert.assertNotNull(z);

            Assert.assertTrue(i.equals(z.from));
        }
    }

    @Test
    public void testInbox() throws InterruptedException {
        runTest(new int[]{}, new int[]{});
        runTest(new int[]{1}, new int[]{});
        runTest(new int[]{2, 3}, new int[]{});
        runTest(new int[]{1}, new int[]{1});
        runTest(new int[]{1}, new int[]{1, 1});
        runTest(new int[]{1}, new int[]{1, 1, 1});
        runTest(new int[]{1, 2}, new int[]{2});
        runTest(new int[]{1, 2}, new int[]{2, 1});
        runTest(new int[]{1, 2}, new int[]{1, 2, 1});
    }
}
