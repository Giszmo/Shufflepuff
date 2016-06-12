package com.shuffle.chan;

import java.io.Serializable;

/**
 * A chan that collects multiple inputs together into one output.
 *
 * Created by Daniel Krawisz on 5/18/16.
 */
public interface Inbox<Address, X extends Serializable> extends Receive<Inbox.Envelope<Address, X>> {

    class Envelope<Address, X> implements Comparable<Envelope<Address, X>> {
        public final Address from;
        public final X payload;
        public final long received;

        Envelope(Address from, X payload) {
            if (from == null || payload == null) throw new NullPointerException();

            this.from = from;
            this.payload = payload;
            this.received = System.currentTimeMillis();
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

    // A send into the inbox is opened, enabling someone to dump messages in it.
    Send<X> receivesFrom(Address from);

    void close() throws InterruptedException;
}
