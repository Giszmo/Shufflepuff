package com.shuffle.chan.packet;

import java.io.Serializable;

/**
 * Created by Daniel Krawisz on 5/24/16.
 */
public class Packet<Address extends Serializable, X extends Serializable> implements Serializable {
    public final SessionIdentifier session;
    public final Address from;
    public final Address to;
    public final int sequenceNumber;
    public final X payload;

    public Packet(SessionIdentifier session, Address from, Address to, int sequenceNumber, X payload) {
        if (session == null || from == null || to == null || payload == null) throw new NullPointerException();

        this.session = session;
        this.from = from;
        this.to = to;
        this.sequenceNumber = sequenceNumber;
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "{" + session + " " + from + " -> " + to + " " + sequenceNumber + " : " + payload + "}";
    }

    @Override
    public int hashCode() {
        return session.hashCode() + 17 * (from.hashCode() + 17 * (to.hashCode() + 17 * (sequenceNumber + 17 * payload.hashCode())));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Packet)) return false;

        Packet p = (Packet)o;

        return session.equals(p.session) && from.equals(p.from) && to.equals(p.to)
                && sequenceNumber == p.sequenceNumber && payload.equals(p.payload);
    }
}
