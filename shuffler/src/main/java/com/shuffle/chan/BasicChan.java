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

    private boolean sendClosed = false;
    private boolean receiveClosed = false;
    private boolean closeSent = false;
    private final LinkedBlockingQueue<Message> queue;

    public BasicChan(int n) {
        if (n < 1) throw new IllegalArgumentException();

        queue = new LinkedBlockingQueue<>(n);
    }

    public BasicChan() {
        queue = new LinkedBlockingQueue<>(1);
    }

    private X receiveMessage(Message m) {
        if (sendClosed && !closeSent) {
            // There is definitely room in the queue at this point for this.
            queue.offer(new Message());
            closeSent = true;
        }

        if (m.x == null) {
            receiveClosed = true;
        }

        return m.x;
    }

    @Override
    public synchronized X receive() throws InterruptedException {
        if (receiveClosed) {
            return null;
        }

        return receiveMessage(queue.take());
    }

    @Override
    public synchronized X receive(long l, TimeUnit u) throws InterruptedException {
        if (receiveClosed) {
            return null;
        }

        Message m = queue.poll(l, u);
        if (m == null) {
            return null;
        }
        return receiveMessage(m);
    }

    @Override
    public boolean send(X x) throws InterruptedException {
        if (x == null) {
            throw new NullPointerException();
        }

        if (sendClosed) return false;

        queue.put(new Message(x));

        return true;
    }

    @Override
    public void close() {
        sendClosed = true;

        closeSent = queue.offer(new Message());
    }

    @Override
    public boolean closed() {
        return receiveClosed;
    }

    @Override
    public String toString() {
        // Oh my god java sucks so bad. I can't even print the type name of X here? I think that's ridiculous.
        return "chan{?}";
    }
}
