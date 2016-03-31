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
public class Chan<X> implements ReceiveChan<X>, SendChan<X> {
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
    private final LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<>();

    private X receiveMessage(Message m) {
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

    private boolean sendMessage(Message x) {
        boolean sent = queue.offer(x);

        if (!sent) {
            receiveClosed = true;
            sendClosed = true;
            return false;
        }

        return true;
    }

    @Override
    public boolean send(X x) {
        if (x == null) {
            throw new NullPointerException();
        }

        return !sendClosed && sendMessage(new Message(x));

    }

    @Override
    public void close() {
        sendClosed = true;

        sendMessage(new Message());
    }

    @Override
    public boolean closed() {
        return receiveClosed;
    }
}
