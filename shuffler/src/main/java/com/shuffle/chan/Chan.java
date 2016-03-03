package com.shuffle.chan;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This class is intended to work just like
 *
 * Created by Daniel Krawisz on 3/3/16.
 */
public class Chan<X> implements ReceiveChan<X>, SendChan<X> {
    volatile boolean closed = false;
    LinkedBlockingQueue<X> queue = new LinkedBlockingQueue<>();

    @Override
    public X receive() {
        if (closed && queue.size() == 0) {
            return null;
        }

        try {
            return queue.take();
        } catch (InterruptedException e) {
            close();
            return null;
        }
    }

    @Override
    public X receive(long l, TimeUnit u) {
        if (closed && queue.size() == 0) {
            return null;
        }

        try {
            return queue.poll(l, u);
        } catch (InterruptedException e) {
            close();
            return null;
        }
    }

    @Override
    public boolean send(X x) {
        if (closed) {
            return false;
        }

        try {
            queue.put(x);
        } catch (InterruptedException e) {
            close();
            return false;
        }

        return true;
    }

    @Override
    public synchronized void close() {
        closed = true;
    }

    @Override
    public boolean closed() {
        return closed;
    }
}
