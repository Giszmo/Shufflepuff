package com.shuffle.chan;

import java.io.Serializable;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A basic inbox.
 *
 * Created by Daniel Krawisz on 5/27/16.
 */
public class BasicInbox<Address, X extends Serializable> implements Inbox<Address, X, X> {

    private static class Transit<Address, X> {
        public final Envelope<Address, X> m;

        private Transit(Envelope<Address, X> m) {
            this.m = m;
        }

        // Used to represent that the channel was closed.
        private Transit() {
            this.m = null;
        }

        @Override
        public String toString() {
            return "Tr[" + m + "]";
        }
    }

    private final LinkedBlockingQueue<Transit<Address, X>> q;

    public BasicInbox(int cap) {
        q = new LinkedBlockingQueue<>(cap);
    }

    private boolean closed = false;
    private boolean closeSent = false;

    private class Receiver implements Send<X> {
        private final Address from;
        private boolean closed = false;

        private Receiver(Address from) {
            this.from = from;
        }

        @Override
        public boolean send(X x) throws InterruptedException {

            return !(closed || BasicInbox.this.closed) &&
                    q.add(new Transit<Address, X>(new Inbox.Envelope<Address, X>(from, x)));
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    // A send into the inbox is opened, enabling someone to dump messages in it.
    @Override
    public synchronized Send<X> receivesFrom(Address from) {
        if (from == null) throw new NullPointerException();

        if (closed) return null;

        return new Receiver(from);
    }

    @Override
    public void close() throws InterruptedException {
        closed = true;
        closeSent = q.offer(new Transit<Address, X>());
    }

    private Envelope<Address, X> receiveMessage(Transit<Address, X> m) {

        if (m == null) return null;

        if (closed && !closeSent) {
            // There is definitely room in the queue because we just removed
            // one element and no more were allowed to be put in.
            q.add(new Transit<Address, X>());
            closeSent = true;
        }

        return m.m;
    }

    @Override
    public Envelope<Address, X> receive() throws InterruptedException {
        if (closed && q.size() == 0) {
            return null;
        }

        return receiveMessage(q.take());
    }

    @Override
    public Envelope<Address, X> receive(long l, TimeUnit u) throws InterruptedException {

        if (closed && q.size() == 0) {
            return null;
        }
        return receiveMessage(q.poll(l, u));
    }

    @Override
    public boolean closed() {
        return closed;
    }

    @Override
    public String toString() {
        return "Inbox[]";
    }
}
