package com.shuffle.chan;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Daniel Krawisz on 4/25/16.
 */
public class HistorySendChan<X> implements SendChan<X> {
    private final SendChan<X> chan;
    private final List<X> history = new LinkedList<>();

    public HistorySendChan(SendChan<X> chan) {
        this.chan = chan;
    }

    @Override
    public boolean send(X x) throws InterruptedException {
        boolean sent = chan.send(x);

        if (sent) {
            history.add(x);
        }

        return sent;
    }

    @Override
    public void close() {
        chan.close();
    }

    public List<X> history() {
        List<X> h = new LinkedList<X>();
        h.addAll(history);
        return h;
    }
}
