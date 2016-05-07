/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;

import com.shuffle.chan.Send;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A single session that acts as a mediator for many virtual sessions.
 *
 * Created by Daniel Krawisz on 1/26/16.
 */
public class Mediator<Name extends Comparable<Name>, Address, Payload> implements Connection<Address, Mediator.Envelope<Name, Payload>> {

    public static class Envelope<Name, Payload> {
        public final Name to; // Null means to the mediator.
        public final Name from; // Null means from the mediator.
        public final Payload payload;

        // The mediator is a virtual channel, so the envelope
        // class has to be able to act like the kind of actions
        // we could take with an ordinary channel. In this case,
        // we can close and open sessions.
        public final boolean openSessionRequest;
        public final boolean openSessionResponse;
        public final boolean closeSession;
        public final boolean register;

        // Create a regular message.
        Envelope(Name from, Name to, Payload payload) {

            if (from == null || payload == null || to == null) {
                throw new NullPointerException();
            }

            this.to = to;
            this.payload = payload;
            this.from = from;

            openSessionRequest = false;
            openSessionResponse = false;
            closeSession = false;
            register = false;
        }

        // Make a registration message.
        Envelope(Name from) {

            if (from == null) {
                throw new NullPointerException();
            }

            this.from = from;

            openSessionRequest = false;
            openSessionResponse = false;
            closeSession = false;
            register = true;

            to = null;
            payload = null;

        }

        // Open/close session messages.
        Envelope(Name from, Name to,
                 boolean openSessionRequest,
                 boolean openSessionResponse,
                 boolean closeSession) {

            if (from == null || to == null) {
                throw new NullPointerException();
            }

            if (!(openSessionRequest || openSessionResponse || closeSession)) {
                throw new IllegalArgumentException();
            }

            this.from = from;
            this.to = to;

            this.openSessionRequest = openSessionRequest;
            this.openSessionResponse = openSessionResponse;
            this.closeSession = closeSession;

            register = false;
            payload = null;
        }
    }

    private static class VirtualConnection<Name extends Comparable<Name>>
            implements Comparable<VirtualConnection<Name>> {

        public final Name a;
        public final Name b;

        private VirtualConnection(Name a, Name b) {
            int compare = a.compareTo(b);

            if (compare == 0) throw new IllegalArgumentException();

            if (compare < 0) {

                this.a = a;
                this.b = b;
            } else {
                this.a = b;
                this.b = a;
            }
        }

        @Override
        public int compareTo(VirtualConnection<Name> c) {
            int compA = a.compareTo(c.a);

            if (compA != 0) {
                return compA;
            }

            return b.compareTo(c.b);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof VirtualConnection)) return false;

            VirtualConnection c = (VirtualConnection)o;

            return a.equals(c.a) && b.equals(c.b);
        }

        @Override
        public int hashCode() {
            return a.hashCode() * 15 + b.hashCode();
        }

        public Name other(Name o) {
            if (a.equals(o)) return b;

            if (b.equals(o)) return a;

            return null;
        }
    }

    private class OpenSessions {
        private final Map<Name, Session<Address, Envelope<Name, Payload>>> openSessions = new HashMap<>();

        private final Set<VirtualConnection<Name>> connections = new HashSet<>();

        private final Map<Name, SortedSet<Name>> pending = new HashMap<>();

        // Drop a peer and all his connections.
        public void drop(Name name) throws InterruptedException {
            SortedSet<Name> notify =  new TreeSet<>();

            // Remove from openSessions
            openSessions.remove(name);

            // Remove from pending.
            SortedSet<Name> p = pending.get(name);
            if (p != null) for (Name n : p) notify.add(n);

            pending.remove(name);

            for (Map.Entry<Name, SortedSet<Name>> entry : pending.entrySet()) {
                notify.add(entry.getKey());

                for (Iterator<Name> i = entry.getValue().iterator(); i.hasNext(); i.next()) {
                    i.remove();
                }
            }

            // Remove from connections.
            for (VirtualConnection<Name> x : connections) {
                Name n = x.other(name);

                if (n != null) {
                    notify.add(n);
                }
            }

            // Now we notify those who had connections with the person we dropped.
            while (!notify.isEmpty()) {
                Name n = notify.first();
                notify.remove(n);

                Session<Address, Envelope<Name, Payload>> s = get(n);
                if (s != null) {
                    s.send(new Envelope<Name, Payload>(name, n, false, false, true));
                }
            }
        }

        public Session<Address, Envelope<Name, Payload>> get(Name name) throws InterruptedException {
            Session<Address, Envelope<Name, Payload>> s = openSessions.get(name);

            if (s == null) return null;

            if (s.closed()) {
                drop(name);
            }

            return null;
        }

        public boolean put(Name name, Session<Address, Envelope<Name, Payload>> s) throws InterruptedException {
            if (s.closed()) return false;

            if (openSessions.containsKey(name)) return false;

            openSessions.put(name, s);

            return true;
        }

        // Initiate connection.
        public boolean initiateConnection(Name a, Name b) throws InterruptedException {
            if (a == null || b == null) throw new NullPointerException();

            if (a.equals(b)) return false;

            // Must be in open sessions.
            if (!openSessions.containsKey(a) || !openSessions.containsKey(b)) return false;

            // Must not be in connections.
            if (connections.contains(new VirtualConnection<>(a, b))) return false;

            // Put in pending.
            SortedSet<Name> l = pending.get(a);
            if (l == null) {
                l = new TreeSet<>();

                pending.put(a, l);
            }

            if (l.contains(b)) return false;
            l.add(b);

            Session<Address, Envelope<Name, Payload>> s = openSessions.get(b);
            if (s.closed()) {
                drop(b);
                return false;
            }

            // Send initiate connection message.
            s.send(new Envelope<Name, Payload>(a, b, true, false, false));
            l.add(b);

            return true;
        }

        // Open a connection between two registered peers.
        public boolean completeConnection(Name a, Name b) throws InterruptedException {
            if (a == null || b == null) throw new NullPointerException();

            if (a.equals(b)) return false;

            // Must not already exist.
            VirtualConnection<Name> check = new VirtualConnection<>(a, b);
            if (connections.contains(check)) return false;

            // Must already be in pending.
            SortedSet<Name> l = pending.get(a);

            if (l == null || !l.contains(b)) return false;

            // Remove from pending.
            l.remove(b);

            // Send response.
            Session<Address, Envelope<Name, Payload>> session = openSessions.get(a);
            if (session == null) {
                drop(a);
                return false;
            }

            session.send(new Envelope<Name, Payload>(b, a, false, true, false));

            // Put in connections.
            connections.add(check);

            return true;
        }

        // Close a connection.
        public boolean close(Name a, Name b) {
            if (a == null || b == null) throw new NullPointerException();

            if (a.equals(b)) return true;

            // Add the connection.
            connections.remove(new VirtualConnection<Name>(a, b));

            return true;
        }

        public boolean send(Envelope<Name, Payload> en) throws InterruptedException {
            if (en.to == null) return false;

            // The message may only be sent if a session exists between the two clients
            // or if this is a close session message.
            if (!(connections.contains(new VirtualConnection<Name>(en.from, en.to)) || en.closeSession)) return false;

            Session<Address, Envelope<Name, Payload>> s = openSessions.get(en.to);

            if (s == null) return false;

            s.send(en);

            return true;
        }
    }

    private final OpenSessions openSessions = new OpenSessions();

    private class MediatorSend implements Send<Envelope<Name, Payload>> {
        Name name = null;
        private final Session<Address, Envelope<Name, Payload>> session;

        // Messages go here until they can be delivered.
        Queue<Payload> queue = new LinkedList<>();

        private MediatorSend(Session<Address, Envelope<Name, Payload>> session) {
            this.session = session;
        }

        @Override
        public synchronized boolean send(Envelope<Name, Payload> en)
                throws InterruptedException {

            // Registration message.
            if (name == null && en.register) {
                name = en.from;
                openSessions.put(name, session);
                return true;
            }

            // If this person isn't registered, ignore messages.
            if (name == null) return false;

            if (en.to == null || en.from != name) return false;

            // This is a normal message.
            if (en.payload != null) {
                return openSessions.send(en);
            }

            // This is a close session request.
            if (en.closeSession) {
                return openSessions.close(en.from, en.to);
            }

            // This is an open session response.
            if (en.openSessionResponse) {
                // Send failure messages to both.
                return openSessions.completeConnection(en.to, name)
                        || openSessions.send(new Envelope<Name, Payload>(en.to, en.from, false, false, true))
                        && openSessions.send(new Envelope<Name, Payload>(en.from, en.to, false, false, true));
            }

            // open session request.
            if (en.openSessionRequest) {
                if (!openSessions.initiateConnection(name, en.to)) {
                    // Send a close session message.
                    return openSessions.send(new Envelope<Name, Payload>(en.to, en.from, false, false, true));
                }
            }
            return false;
        }

        @Override
        public synchronized void close() throws InterruptedException {
            conn.close();
        }
    }

    private class MediatorListener implements Listener<Address, Envelope<Name, Payload>> {

        @Override
        public Send<Envelope<Name, Payload>> newSession(
                Session<Address, Envelope<Name, Payload>> session
        ) throws InterruptedException {

            if (session == null) throw new NullPointerException();

            return new MediatorSend(session);
        }
    }

    Channel<Address, Envelope<Name, Payload>> clients;
    private final Connection<Address, Envelope<Name, Payload>> conn;

    public Mediator(Channel<Address, Envelope<Name, Payload>> clients) throws InterruptedException {
        this.clients = clients;
        conn = clients.open(new MediatorListener());

        if (conn == null) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public Address identity() {
        return conn.identity();
    }

    @Override
    public void close() throws InterruptedException {
        conn.close();
    }

}
