/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.mock;

import com.shuffle.chan.Send;
import com.shuffle.p2p.Channel;
import com.shuffle.p2p.Connection;
import com.shuffle.p2p.FundamentalPeer;
import com.shuffle.p2p.Listener;
import com.shuffle.p2p.Peer;
import com.shuffle.p2p.Session;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Mock channel for testing purposes. 
 *
 * Created by Daniel Krawisz on 3/2/16.
 */
public class MockChannel<Q, X> implements Channel<Q, X> {
    final Map<Q, MockChannel<Q, X>> knownHosts;
    final Map<Q, MockPeer> peers = new HashMap<>();
    MockConnection connection;
    Listener<Q, X> listener;

    public MockChannel(Q me, Map<Q, MockChannel<Q, X>> knownHosts) {
        this.knownHosts = knownHosts;
        this.me = me;
    }

    class MockConnection implements Connection<Q, X> {

        @Override
        public Q identity() {
            return me;
        }

        @Override
        public void close() throws InterruptedException {
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

    public class MockPeer extends FundamentalPeer<Q, X> {

        MockPeer(Q you) {
            super(you);
        }

        Session<Q, X> getSession() {
            return currentSession;
        }

        void setSession(Session<Q, X> session) {
            currentSession = session;
        }

        @Override
        public String toString() {
            return "MockPeer[" + identity() + "]";
        }

        @Override
        // Open a session with a mock remote peer. Include a function which is to be
        // called when a X is received.
        public synchronized Session<Q, X> openSession(Send<X> send)
                throws InterruptedException {

            if (send == null) throw new NullPointerException();

            // if there is already an open session, fail.
            if (currentSession != null) return null;

            Q identity = identity();

            // Do we know this remote peer?
            MockChannel<Q, X> remote = knownHosts.get(identity);
            if (remote == null) return null;
            if (!equals(peers.get(identity))) return null;

            // Create a new session and register it with the remote peer.
            Send<X> r = remote.connect(me, send);
            if (r == null) return null;

            MockSession session = this.new MockSession(r);

            // If the session is not open, the connection didn't work for some reason.
            if (session.closed()) return null;

            // Set the new session as the officially connected one for this peer.
            this.currentSession = session;
            return session;
        }

        public class MockSession implements Session<Q, X> {
            final Send<X> send;
            boolean closed;

            MockSession(Send<X> send) {

                if (send == null) throw new NullPointerException();

                this.send = send;
                closed = false;
            }

            @Override
            public boolean send(X x) throws InterruptedException {
                System.out.println("(mock) Sending message " + x);
                return !closed && send.send(x);
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
            public Peer<Q, X> peer() {
                return MockPeer.this;
            }
        }
    }

    final Q me;

    @Override
    public Peer<Q, X> getPeer(Q you) {
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
    public Connection<Q, X> open(Listener<Q, X> listener) {

        if (this.listener != null) throw new NullPointerException();
        this.listener = listener;

        this.connection = new MockConnection();
        return this.connection;
    }

    Send<X> connect(Q you, Send<X> send) throws InterruptedException {
        Thread.sleep(100);

        if (you == null || send == null) throw new NullPointerException();

        if (listener == null) return null;

        // Do we know this remote peer?
        MockPeer peer = (MockPeer) getPeer(you);
        if (peer == null) return null;

        // An open session already exists.
        if (peer.open()) return null;

        peer.setSession(peer.new MockSession(send));

        return listener.newSession(peer.getSession());
    }

}
