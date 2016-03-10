package com.shuffle.chan;

/**
 * Created by Daniel Krawisz on 3/3/16.
 */
public interface SendChan<X> {
    boolean send(X x);
    void close();
}
