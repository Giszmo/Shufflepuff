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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Mock channel for testing purposes. 
 *
 * Created by Daniel Krawisz on 3/2/16.
 */
public class MockNetwork<Q, X extends Serializable> {

    final Map<Q, MockChannel> knownHosts = new ConcurrentHashMap<>();

    // Locks for making connections. It's a symmetric tensor with
    // zero diagonal.
    final Map<Q, Map<Q, Lock>> locks = new ConcurrentHashMap<>();

    public synchronized Channel<Q, X> node(Q q) {

        if (knownHosts.containsKey(q)) return null;

        Map<Q, Lock> l = new ConcurrentHashMap<>();

        for (Map.Entry<Q, Map<Q, Lock>> lm : locks.entrySet()) {
            Lock lock = new ReentrantLock();

            lm.getValue().put(q, lock);
            l.put(lm.getKey(), lock);
        }

        locks.put(q, l);

        MockChannel chq = new MockChannel(q);
        knownHosts.put(q, chq);
        return chq;
    }

    class MockChannel implements Channel<Q, X> {
        final Map<Q, MockPeer> peers = new ConcurrentHashMap<>();
        Listener<Q, X> listener;

        public MockChannel(Q me) {
            this.me = me;
        }

        class MockConnection implements Connection<Q> {
            boolean closed = false;

            @Override
            public Q identity() {
                return me;
            }

            @Override
            public void close() throws InterruptedException {
                synchronized (lock) {
                    if (closed || MockChannel.this.closed) {
                        return;
                    }

                    for (MockPeer peer : peers.values()) {
                        peer.close();
                    }

                    peers.clear();

                    listener = null;
                    closed = true;
                    MockChannel.this.closed = true;
                }
            }

            @Override
            public boolean closed() {
                return closed;
            }

            @Override
            public String toString() {
                return "MockConnection[" + me + "]";
            }
        }

        private final class MockPeer extends FundamentalPeer<Q, X> {

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

                synchronized (lock) {}
                if (send == null) throw new NullPointerException();

                // if there is already an open session, fail.
                if (currentSession != null) return null;

                Q identity = identity();

                // Do we know this remote peer?
                MockChannel remote = knownHosts.get(identity);
                if (remote == null) return null;
                if (!equals(peers.get(identity))) return null;

                // Create a new session and register it with the remote peer.
                Send<X> r = null;

                Lock lock = locks.get(me).get(identity);
                if(lock.tryLock(37, TimeUnit.MILLISECONDS)) {
                    r = remote.connect(me, send);
                    lock.unlock();
                } else {
                    return null;
                }
                if (r == null) return null;

                MockSession session = this.new MockSession(r);

                // If the session is not open, the connection didn't work for some reason.
                if (session.closed()) return null;

                // Set the new session as the officially connected one for this peer.
                this.currentSession = session;
                return session;
            }

            @Override
            public boolean equals(Object o) {
                if (o == null || !(o instanceof MockNetwork.MockChannel.MockPeer)) return false;

                MockNetwork.MockChannel.MockPeer m = (MockNetwork.MockChannel.MockPeer)o;

                // TODO check whether the channels and the network as a whole are the same.
                return identity().equals(m.identity());
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

                @Override
                public String toString() {
                    return "MockNetwork[" + me + " => " + identity() + "]";
                }
            }
        }

        final Q me;
        private final Object lock = new Object();
        private boolean closed = true;

        @Override
        public Q identity() {
            return me;
        }

        @Override
        public Peer<Q, X> getPeer(Q you) {
            // Can't establish a connection to myself or a peer we don't know about.
            if (you.equals(me) || !knownHosts.containsKey(you)) return null;

            MockPeer peer = peers.get(you);

            if (peer == null) {
                peer = new MockPeer(you);

                peers.put(you, peer);
            }

            return peer;
        }

        @Override
        public Connection<Q> open(Listener<Q, X> listener) {
            synchronized (lock) {
                if (!closed) return null;

                if (this.listener != null) throw new NullPointerException();
                this.listener = listener;

                closed = false;
                return new MockConnection();
            }
        }

        public Send<X> connect(Q you, Send<X> send) throws InterruptedException {
            synchronized (lock) {
                if (closed) return null;
            }

            if (you == null || send == null) throw new NullPointerException();

            if (listener == null) return null;

            // Do we know this remote peer?
            MockPeer peer = (MockPeer) getPeer(you);
            if (peer == null) return null;

            // An open session already exists.
            if (peer.open()) return null;

            Session<Q, X> s = peer.new MockSession(send);
            peer.setSession(s);

            Send<X> l = listener.newSession(peer.getSession());
            if (l == null) {
                s.close();
                return null;
            }

            return l;
        }

    }

}
