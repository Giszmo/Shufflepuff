/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.chan;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A chan class made to work similar to the chan type in golang.
 *
 * X should be an immutable object.
 *
 * Created by Daniel Krawisz on 3/3/16.
 */
public class BasicChan<X> implements Chan<X> {

    // Need a kind of message to indicate that a channel has been closed.
    private class Message {
        public X x;

        Message() {
            x = null;
        }

        Message(X x) {
            if (x == null) {
                throw new NullPointerException();
            }
            this.x = x;
        }
    }

    private boolean closed = false;
    private boolean closeSent = false;
    private final LinkedBlockingQueue<Message> q;

    private final Object lock = new Object();

    public BasicChan(int n) {
        if (n < 1) throw new IllegalArgumentException();

        q = new LinkedBlockingQueue<>(n);
    }

    public BasicChan() {
        q = new LinkedBlockingQueue<>(1);
    }

    private X receiveMessage(Message m) {
        if (closed && !closeSent) {
            // There is definitely room in the queue because we just removed
            // one element and no more were allowed to be put in.
            q.add(new Message());
            closeSent = true;
        }

        return m.x;
    }

    @Override
    public X receive() throws InterruptedException {
        if (closed && q.size() == 0) {

            return null;
        }

        return receiveMessage(q.take());
    }

    @Override
    public X receive(long l, TimeUnit u) throws InterruptedException {
        if (closed && q.size() == 0) {
            return null;
        }

        return receiveMessage(q.poll(l, u));
    }

    @Override
    public synchronized boolean send(X x) throws InterruptedException {
        if (x == null) {
            throw new NullPointerException();
        }

        if (closed) return false;

        q.put(new Message(x));

        return true;
    }

    @Override
    public synchronized void close() {
        closed = true;

        closeSent = q.offer(new Message());
    }

    @Override
    public boolean closed() {
        return closed && q.size() == 0;
    }

    @Override
    public String toString() {
        // Oh my god java sucks so bad. I can't even print the type name of X here? I think that's ridiculous.
        return "chan{?}";
    }
}
