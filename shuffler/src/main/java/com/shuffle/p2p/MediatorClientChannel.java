package com.shuffle.p2p;

import com.shuffle.chan.BasicChan;
import com.shuffle.chan.Chan;
import com.shuffle.chan.Send;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A virtual connection in which some peer to which we are connected
 * passes on our messages to a bunch of other people via some
 * communication channel that we aren't able to use.
 *
 * Created by Eugene Siegel on 4/12/16.
 */

public class MediatorClientChannel<Name, Address, Payload> implements Channel<Name, Payload> {

    // Methods for creating Envelope objects.
    // (use these for creating Envelope objects for safety.)

    // Make a standard peer message.
    public Mediator.Envelope<Name, Payload> PeerMessage(Name to, Payload payload) {
        return new Mediator.Envelope<Name, Payload>(me, to, payload);
    }

    public Mediator.Envelope<Name, Payload> ServerRegistration() {
        return new Mediator.Envelope<Name, Payload>(me);
    }

    public Mediator.Envelope<Name, Payload> OpenSessionRequest(Name to) {
        return new Mediator.Envelope<Name, Payload>(me, to, true, false, false);
    }

    public Mediator.Envelope<Name, Payload> OpenSessionResponse(Name to) {
        return new Mediator.Envelope<Name, Payload>(me, to, false, true, false);
    }

    public Mediator.Envelope<Name, Payload> CloseSession(Name to) {
        return new Mediator.Envelope<Name, Payload>(me, to, false, false, true);
    }

    // This is the remote peer to which we are connected when the channel is open.
    private final Peer<Address, Mediator.Envelope<Name, Payload>> virtualChannel;

    // This is the corresponding session.
    private Session<Address, Mediator.Envelope<Name, Payload>> session;

    private final Name me;
    private final Object lock = new Object();

    private class OpenSessions {
        private final Object lock = new Object();

        private class PendingSession {
            public final Send<Payload> send;
            public final Chan<Void> connected;

            private PendingSession(Send<Payload> send, Chan<Void> connected) {
                this.send = send;
                this.connected = connected;
            }
        }

        private final ConcurrentMap<Name, Send<Payload>> openSessions = new ConcurrentHashMap<>();
        private final ConcurrentMap<Name, PendingSession> pendingSessions = new ConcurrentHashMap<>();

        // Halts if a session is pending until it is completed.
        public Send<Payload> get(Name name) throws InterruptedException {
            return openSessions.get(name);
        }

        public boolean connected(Name name) throws InterruptedException {
            PendingSession p = pendingSessions.get(name);

            if (p != null) {
                p.connected.receive();
            }

            return openSessions.containsKey(name);
        }

        public boolean openInitiate(Name you, Send<Payload> r) throws InterruptedException {
            synchronized (lock) {
                if (pendingSessions.containsKey(you) || openSessions.containsKey(you)) return false;

                // Put in pending sessions.
                pendingSessions.put(you, new PendingSession(r, new BasicChan<Void>(0)));
            }

            session.send(OpenSessionRequest(you));

            return true;
        }

        boolean openRespond(Name you, Listener<Name, Payload> listener) throws InterruptedException {
            synchronized (lock) {
                if (pendingSessions.containsKey(you) || openSessions.containsKey(you)) return false;

                Send<Payload> r = listener.newSession(new MediatorClientSession(you));

                if (r == null) return false;

                openSessions.put(you, r);
                return true;
            }
        }

        public boolean openComplete(Name from) throws InterruptedException {
            synchronized (lock) {
                PendingSession p = pendingSessions.get(from);

                if (p == null) return false;

                openSessions.put(from, p.send);

                p.connected.close();
            }

            return true;
        }

        public boolean remove(Name you) throws InterruptedException {
            synchronized (lock) {
                PendingSession p = pendingSessions.get(you);

                if (p != null) {
                    pendingSessions.remove(you);

                    p.connected.close();
                    return true;
                }

                Send<Payload> r = openSessions.get(you);

                if (r != null) {
                    openSessions.remove(you);

                    return true;
                }
            }

            return false;
        }

        public void close(Name you) throws InterruptedException {
            if (remove(you)) {
                session.send(CloseSession(you));
            }
        }

        public void clear() throws InterruptedException {
            for (Map.Entry<Name, PendingSession> entry : pendingSessions.entrySet()) {
                pendingSessions.remove(entry.getKey());

                session.send(CloseSession(entry.getKey()));
                entry.getValue().connected.close();

            }

            for (Name name : openSessions.keySet()) {
                openSessions.remove(name);

                session.send(CloseSession(name));
            }
        }
    }

    private final OpenSessions openSessions = new OpenSessions();

    public MediatorClientChannel(Name me, final Peer<Address, Mediator.Envelope<Name, Payload>> peer) {
        this.me = me;
        this.virtualChannel = peer;
    }

    private class MediatorClientPeer implements Peer<Name, Payload> {
        private final Name you;

        MediatorClientPeer(Name you) {
            this.you = you;
        }

        @Override
        public Name identity() {
            return you;
        }

        @Override
        public Session<Name, Payload> openSession(Send<Payload> send)
                throws InterruptedException {

            // If we are already connected, don't open a new one.
            if (openSessions.connected(you)) return null;

            if (!openSessions.openInitiate(you, send)) return null;

            if (!openSessions.connected(you)) return null;

            return new MediatorClientSession(you);
        }

        @Override
        public boolean open() throws InterruptedException {
            return openSessions.connected(you);
        }

        @Override
        public void close() throws InterruptedException {
            openSessions.close(you);
        }
    }

    private class MediatorClientSession implements Session<Name, Payload> {
        private final Name you;

        private MediatorClientSession(Name you) {
            this.you = you;
        }

        @Override
        public boolean closed() throws InterruptedException {

            return !openSessions.connected(you);
        }

        @Override
        public Peer<Name, Payload> peer() {
            return new MediatorClientPeer(you);
        }

        @Override
        public boolean send(Payload payload) throws InterruptedException {
            return session.send(PeerMessage(you, payload));
        }

        @Override
        public void close() throws InterruptedException {
            new MediatorClientPeer(you).close();
        }
    }

    private void close() throws InterruptedException {
        if (session == null) return;

        synchronized (lock) {

            openSessions.clear();

            session.close();
            session = null;
        }
    }

    private class MediatorClientConnection implements Connection<Name, Payload> {

        @Override
        public Name identity() {
            return me;
        }

        @Override
        public void close() throws InterruptedException {
            MediatorClientChannel.this.close();
        }
    }

    private class MediatorClientSend implements Send<Mediator.Envelope<Name, Payload>> {

        private final Listener<Name, Payload> listener;

        private MediatorClientSend(Listener<Name, Payload> listener) {
            this.listener = listener;
        }

        @Override
        public boolean send(Mediator.Envelope<Name, Payload> envelope) throws InterruptedException {

            synchronized (lock) {
                if (session == null) return false;

                if (!me.equals(envelope.to)) {
                    return false;
                }

                // Is this a regular message?
                Send<Payload> r = openSessions.get(envelope.from);
                if (r != null && envelope.payload != null) {

                    return r.send(envelope.payload);
                }

                // Is this a request to open a session?
                if (envelope.openSessionRequest) {
                    return openSessions.openRespond(envelope.from, listener);
                }

                // Is this a response from the server to an open session request?
                if (envelope.openSessionResponse) {
                    return openSessions.openComplete(envelope.from);
                }

                // Is it a close session message?
                if (envelope.closeSession) {
                    return openSessions.remove(envelope.from);
                }

                return false;
            }
        }

        @Override
        public void close() throws InterruptedException {
            MediatorClientChannel.this.close();
        }
    }

    public Connection<Name, Payload> open(final Listener<Name, Payload> listener)
            throws InterruptedException {

        synchronized (lock) {
            session = virtualChannel.openSession(new MediatorClientSend(listener));

            if (session == null) return null;

            return new MediatorClientConnection();
        }
    }


    public Peer<Name,Payload> getPeer(Name you) {
        return new MediatorClientPeer(you);
    }
}
