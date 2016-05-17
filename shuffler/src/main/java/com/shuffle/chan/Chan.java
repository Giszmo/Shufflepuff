package com.shuffle.chan;

/**
 * A channel that can be used for both sending and receiving.
 *
 * Created by Daniel Krawisz on 4/14/16.
 */
public interface Chan<X> extends Receive<X>, Send<X> {
}
