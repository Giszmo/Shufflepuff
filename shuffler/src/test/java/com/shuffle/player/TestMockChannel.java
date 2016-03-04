package com.shuffle.player;

import com.shuffle.p2p.Connection;
import com.shuffle.p2p.Listener;
import com.shuffle.p2p.Receiver;
import com.shuffle.p2p.Session;
import com.shuffle.protocol.Message;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * You know things are bad when you need unit tests for your mock objects.
 *
 * Created by Daniel Krawisz on 3/4/16.
 */
public class TestMockChannel {
    public class MockListener implements Listener<Integer, String> {
        public final Map<Integer, Session<Integer, String>> openSessions = new HashMap<>();
        public final Map<Integer, Receiver<String>> receivers = new HashMap<>();

        public Receiver<String> getReceiver(Integer from) {
            Receiver<String> receiver = receivers.get(from);
            if (receiver == null) {
                receiver = new MockReceiver();
                receivers.put(from, receiver);
            }
            return receiver;
        }

        @Override
        public Receiver<String> newSession(Session<Integer, String> session) {
            return getReceiver(session.peer().identity());
        }
    }

    public class MockReceiver implements Receiver<String> {
        List<String> messages = new LinkedList<String>();

        @Override
        public void receive(String s) {
            messages.add(s);
        }
    }

    @Test
    public void testMockChannel() throws InterruptedException {
        // Create channels.
        Map<Integer, MockChannel<String>> knownHosts = new HashMap<>();
        Map<Integer, Connection<Integer, String>> connections = new HashMap<>();
        Map<Integer, MockListener> listeners = new HashMap<>();
        Map<Integer, MockReceiver> receivers = new HashMap<>();
        knownHosts.put(1, new MockChannel<String>(1, knownHosts));
        knownHosts.put(2, new MockChannel<String>(2, knownHosts));
        listeners.put(1, new MockListener());
        listeners.put(2, new MockListener());
        receivers.put(1, new MockReceiver());
        receivers.put(2, new MockReceiver());

        connections.put(1, knownHosts.get(1).open(listeners.get(1)));
        connections.put(2, knownHosts.get(2).open(listeners.get(2)));

        Assert.assertNotNull(connections.get(1));
        Assert.assertNotNull(connections.get(2));

        // Can't have myself as a peer.
        Assert.assertNull(knownHosts.get(1).getPeer(1));
        Assert.assertNull(knownHosts.get(2).getPeer(2));

        // Can't have unknown peers
        Assert.assertNull(knownHosts.get(1).getPeer(3));
        Assert.assertNull(knownHosts.get(2).getPeer(3));

        // Can get these peers.
        Assert.assertNotNull(knownHosts.get(1).getPeer(2));
        Assert.assertNotNull(knownHosts.get(2).getPeer(1));

        // No open sessions.
        Assert.assertFalse(knownHosts.get(1).getPeer(2).open());
        Assert.assertFalse(knownHosts.get(2).getPeer(1).open());

        // Try to open sessions.
        Assert.assertNotNull(knownHosts.get(1).getPeer(2).openSession(receivers.get(1)));
        Assert.assertNull(knownHosts.get(1).getPeer(2).openSession(receivers.get(1)));
        Assert.assertNull(knownHosts.get(2).getPeer(1).openSession(receivers.get(2)));
        Assert.assertTrue(knownHosts.get(2).getPeer(1).open());

        // Now the sessions should be opened.
        Assert.assertTrue(knownHosts.get(1).getPeer(2).open());
        Assert.assertTrue(knownHosts.get(2).getPeer(1).open());

        connections.get(1).close();
        connections.get(2).close();

        // Avoid memory leak.
        knownHosts.clear();
    }
}
