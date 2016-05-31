package com.shuffle.chan;

/**
 * IgnoreSend is a channel that leads nowhere.
 *
 * TODO: I don't think that this class is actually a good idea.
 * It should be removed in the future. 
 *
 * Created by Daniel Krawisz on 5/26/16.
 */
public class IgnoreSend<X> implements Send<X> {
    @Override
    public boolean send(X x) throws InterruptedException {
        return true;
    }

    @Override
    public void close() throws InterruptedException {}
}
