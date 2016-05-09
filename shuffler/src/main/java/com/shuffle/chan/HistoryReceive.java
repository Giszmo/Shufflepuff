package com.shuffle.chan;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Daniel Krawisz on 4/25/16.
 */
public class HistoryReceive<X> implements Receive<X> {
    private final Receive<X> chan;
    private final List<X> history = new LinkedList<>();

    public HistoryReceive(Receive<X> chan) {
        this.chan = chan;
    }

    @Override
    public X receive() throws InterruptedException {
        X x = chan.receive();
        if (x != null) {
            history.add(x);
        }
        return x;
    }

    @Override
    public X receive(long l, TimeUnit u) throws InterruptedException {
        X x = chan.receive(l, u);
        if (x != null) {
            history.add(x);
        }
        return x;
    }

    @Override
    public boolean closed() {
        return chan.closed();
    }

    public List<X> history() {
        List<X> h = new LinkedList<>();
        h.addAll(history);
        return h;
    }
}
