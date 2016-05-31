package com.shuffle.p2p;

import com.shuffle.chan.Inbox;
import com.shuffle.chan.Send;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The Collector is a standard listener that puts all new sessions together in one map.
 *
 * Created by Daniel Krawisz on 5/30/16.
 */
public class Collector<Address, X extends Serializable> implements Listener<Address, X> {
    // The set of connected peers. This member and the next are ipublic but ultimately
    // the program should be refactored so that they don't need to be public.
    public final ConcurrentMap<Address, Send<X>> connected = new ConcurrentHashMap<>();

    public final Inbox<Address, X, X> inbox;

    public Collector(Inbox<Address, X, X> inbox) {
        if (inbox == null) throw new NullPointerException();
        if (inbox.closed()) throw new IllegalArgumentException();

        this.inbox = inbox;
    }

    public Send<X> get(Address address) {
        return connected.get(address);
    }

    public boolean put(Session<Address, X> session) throws InterruptedException {
        if (session == null) throw new NullPointerException();
        Address address = session.peer().identity();

        if (session.closed()) return false;

        if (connected.putIfAbsent(address, session) != null) {
            session.close();
            return false;
        }

        return true;
    }

    @Override
    public Send<X> newSession(Session<Address, X> session) throws InterruptedException {
        if (session == null) throw new NullPointerException();

        if (!put(session)) return null;

        return inbox.receivesFrom(session.peer().identity());
    }

    @Override
    public String toString() {
        return "messages{ connected: " + connected + ", inbox: " + inbox + "}";
    }
}
