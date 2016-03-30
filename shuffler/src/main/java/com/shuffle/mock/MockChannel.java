/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.mock;

import com.shuffle.p2p.Channel;
import com.shuffle.p2p.Connection;
import com.shuffle.p2p.FundamentalPeer;
import com.shuffle.p2p.Listener;
import com.shuffle.p2p.Peer;
import com.shuffle.p2p.Receiver;
import com.shuffle.p2p.Session;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock channel for testing purposes.
 *
 * Created by Daniel Krawisz on 3/2/16.
 */
public class MockChannel<X> implements Channel<Integer, X> {
    final Map<Integer, MockChannel<X>> knownHosts;
    Map<Integer, MockPeer> peers = new HashMap<>();
    MockConnection connection;
    Listener<Integer, X> listener;

    public MockChannel(Integer me, Map<Integer, MockChannel<X>> knownHosts) {
        this.knownHosts = knownHosts;
        this.me = me;
    }

    class MockConnection implements Connection<Integer, X> {

        @Override
        public void close() {
            if (connection == null) {
                return;
            }

            for (MockPeer peer : peers.values()) {
                peer.close();
            }

            peers.clear();

            connection = null;
            listener = null;
        }
    }

    public class MockPeer extends FundamentalPeer<Integer, X> {

        MockPeer(Integer you) {
            super(you);
        }

        Session<Integer, X> getSession() {
            return currentSession;
        }

        void setSession(Session<Integer, X> session) {
            currentSession = session;
        }

        @Override
        public String toString() {
            return "MockPeer[" + identity() + "]";
        }

        @Override
        // Open a session with a mock remote peer. Include a function which is to be
        // called when a X is received.
        public synchronized Session<Integer, X> openSession(Receiver<X> receiver)
                throws InterruptedException {
            // if there is already an open session, fail.
            if (currentSession != null) {
                return null;
            }

            Integer identity = identity();

            // Do we know this remote peer?
            MockChannel<X> remote = knownHosts.get(identity);
            if (remote == null) {
                return null;
            }

            if (!equals(peers.get(identity))) {
                return null;
            }

            // Create a new session and register it with the remote peer.
            MockSession session = this.new MockSession(remote.connect(me, receiver));

            // If the session is not open, the connection didn't work for some reason.
            if (session.closed()) {
                return null;
            }

            // Set the new session as the officially connected one for this peer.
            this.currentSession = session;
            return session;
        }

        public class MockSession implements Session<Integer, X> {
            Receiver<X> receiver;
            boolean closed;

            MockSession(Receiver<X> receiver) {
                this.receiver = receiver;
                closed = receiver == null;
            }

            @Override
            public boolean send(X x) {
                if (receiver == null) {
                    return false;
                }

                receiver.receive(x);
                return true;
            }

            @Override
            public void close() {
                closed = true;
                MockPeer.this.currentSession = null;
            }

            @Override
            public boolean closed() {
                return closed;
            }

            @Override
            public Peer<Integer, X> peer() {
                return MockPeer.this;
            }
        }
    }

    final Integer me;

    @Override
    public Peer<Integer, X> getPeer(Integer you) {
        // Can't establish a connection to myself.
        if (you.equals(me)) {
            return null;
        }

        MockPeer peer = peers.get(you);

        if (peer == null && knownHosts.containsKey(you)) {
            peer = new MockPeer(you);

            peers.put(you, peer);
        }

        return peer;
    }

    @Override
    public Connection<Integer, X> open(Listener<Integer, X> listener) {
        if (this.listener != null) {
            return null;
        }
        this.listener = listener;

        this.connection = new MockConnection();
        return this.connection;
    }

    Receiver<X> connect(Integer you, Receiver<X> receiver) throws InterruptedException {
        Thread.sleep(100);

        if (listener == null || receiver == null) {
            return null;
        }

        // Do we know this remote peer?
        MockPeer peer = (MockPeer) getPeer(you);
        if (peer == null) {
            return null;
        }

        // An open session already exists.
        if (peer.open()) {
            return null;
        }

        peer.setSession(peer.new MockSession(receiver));

        return listener.newSession(peer.getSession());
    }
}
