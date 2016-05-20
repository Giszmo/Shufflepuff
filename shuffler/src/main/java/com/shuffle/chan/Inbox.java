package com.shuffle.chan;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A chan that collects multiple inputs together into one output.
 *
 * Created by Daniel Krawisz on 5/18/16.
 */
public class Inbox<Address, X> implements Receive<Inbox.Envelope<Address, X>> {

    public static class Envelope<Address, X> implements Comparable<Envelope<Address, X>> {
        public final Address from;
        public final X payload;
        public final long received;

        private Envelope(Address from, X payload) {
            if (from == null || payload == null) throw new NullPointerException();

            this.from = from;
            this.payload = payload;
            this.received = System.currentTimeMillis();
        }

        // Signifies a channel closed message.
        private Envelope() {
            from = null;
            payload = null;
            received = 0;
        }

        @Override
        public int compareTo(Envelope<Address, X> envelope) {
            if (received == envelope.received) return 0;

            if (received < envelope.received) return -1;

            return 1;
        }

        @Override
        public String toString() {
            return "[ received: " + received + "; from: " + from + "; contents: " + payload + " ]";
        }
    }

    private final LinkedBlockingQueue<Envelope<Address, X>> q;

    public Inbox(int cap) {
        q = new LinkedBlockingQueue<>(cap);
    }

    private final Set<Address> sessions = new TreeSet<>();

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
            return !(closed || Inbox.this.closed) && q.add(new Envelope<Address, X>(from, x));
        }

        @Override
        public void close() {
            closed = true;
            sessions.remove(from);
        }
    }

    // A send into the inbox is opened, enabling someone to dump messages in it.
    public Send<X> receivesFrom(Address from) {
        if (from == null) throw new NullPointerException();

        if (closed) return null;

        if (sessions.contains(from)) {
            return null;
        }

        sessions.add(from);
        return new Receiver(from);
    }

    public void close() throws InterruptedException {
        closed = true;
        closeSent = q.offer(new Envelope<Address, X>());
    }

    private Envelope<Address, X> receiveMessage(Envelope<Address, X> m) {
        if (m == null) return null;

        if (closed && !closeSent) {
            // There is definitely room in the queue because we just removed
            // one element and no more were allowed to be put in.
            q.add(new Envelope<Address, X >());
            closeSent = true;
        }

        if (m.from == null || m.payload == null || m.received == 0) return null;

        return m;
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
}
