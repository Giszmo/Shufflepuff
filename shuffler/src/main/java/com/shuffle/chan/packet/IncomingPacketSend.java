package com.shuffle.chan.packet;

import com.shuffle.chan.Send;

import java.io.Serializable;

/**
 * IncomingPacketSend checks incoming packets to see that they have the correct
 * values for from, to, session, and sequenceNumber
 *
 * Created by Daniel Krawisz on 5/24/16.
 */
public class IncomingPacketSend<Address extends Serializable, X extends Serializable> implements Send<Packet<Address, X>> {
    private final Send<Packet<Address, X>> send;

    private final SessionIdentifier session;
    private final Address from, to;

    int sequenceNumber = 0;
    boolean closed = false;

    public IncomingPacketSend(Send<Packet<Address, X>> send, SessionIdentifier session, Address from, Address to) {
        if (send == null || session == null || from == null || to == null) throw new NullPointerException();

        this.send = send;
        this.session = session;
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean send(Packet<Address, X> x) throws InterruptedException {
        if (closed) return false;

        if (!session.equals(x.session) || !from.equals(x.from)
                || !to.equals(x.to) || sequenceNumber != x.sequenceNumber) {

            return false;
        }

        boolean sent = send.send(x);

        if (sent) sequenceNumber ++;

        return sent;
    }

    @Override
    public void close() throws InterruptedException {
        if (!closed) {
            send.close();
            closed = true;
        }
    }
}
