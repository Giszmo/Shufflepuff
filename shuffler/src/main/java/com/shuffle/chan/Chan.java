package com.shuffle.chan;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This class is intended to work similar to the chan type in golang.
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

    boolean sendClosed = false;
    boolean receiveClosed = false;
    LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<>();

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
