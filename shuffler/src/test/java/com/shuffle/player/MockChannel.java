package com.shuffle.player;

import com.shuffle.p2p.Bytestring;
import com.shuffle.p2p.Channel;
import com.shuffle.p2p.Connection;
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
public class MockChannel implements Channel<Integer, Bytestring> {
    final Map<Integer, MockChannel> knownHosts;
    Map<Integer, MockPeer.MockSession> openSessions = new HashMap<>();
    Map<Integer, MockPeer> peers = new HashMap<>();
    MockConnection connection;
    Listener<Integer, Bytestring> listener;

    public MockChannel(Integer me, Map<Integer, MockChannel> knownHosts) {
        this.knownHosts = knownHosts;
        this.me = me;
    }

    class MockConnection implements Connection<Integer, Bytestring> {

        @Override
        public void close() {
            if (connection == null) {
                return;
            }

            for(MockPeer.MockSession session : openSessions.values()) {
                session.close();
            }

            openSessions.clear();

            connection = null;
            listener = null;
        }
    }

    class MockPeer implements Peer<Integer, Bytestring> {
        final Integer you;
        MockSession session;

        MockPeer(Integer you) {
            this.you = you;
        }

        @Override
        public Integer identity() {
            return null;
        }

        @Override
        public String toString() {
            return "MockPeer[" + you + "]";
        }

        @Override
        public Session<Integer, Bytestring> openSession(Receiver<Bytestring> receiver) {
            if (session != null) {
                return null;
            }

            MockChannel remote = knownHosts.get(you);
            if (remote == null) {
                return null;
            }

            MockSession session = new MockSession();

            session.receiver = remote.connect(session);

            if (session.receiver == null) {
                return null;
            }

            this.session = session;
            return session;
        }

        void closeSession() {
            if (session == null) {
                return;
            }

            session = null;
            openSessions.remove(you);
        }

        class MockSession implements Session<Integer, Bytestring> {
            Receiver<Bytestring> receiver;

            MockSession() { }

            MockSession(Receiver<Bytestring> receiver) {
                this.receiver = receiver;
            }

            @Override
            public boolean send(Bytestring bytestring) {
                if (receiver == null) {
                    return false;
                }

                receiver.receive(bytestring);
                return true;
            }

            @Override
            public void close() {
                closeSession();
                receiver = null;
            }

            @Override
            public Peer<Integer, Bytestring> peer() {
                return MockPeer.this;
            }
        }
    }

    final Integer me;

    @Override
    public Peer<Integer, Bytestring> getPeer(Integer you) {
        MockPeer peer = peers.get(you);

        if (peer == null && knownHosts.containsKey(you)) {
            peer = new MockPeer(you);

            peers.put(you, peer);
        }

        return peer;
    }

    @Override
    public Connection<Integer, Bytestring> open(Listener<Integer, Bytestring> listener) {
        if (this.listener != null) {
            return null;
        }
        this.listener = listener;

        this.connection = new MockConnection();
        return this.connection;
    }

    Receiver<Bytestring> connect(Session<Integer, Bytestring> session) {
        if (listener == null) {
            return null;
        }

        return listener.newSession(session);
    }
}
