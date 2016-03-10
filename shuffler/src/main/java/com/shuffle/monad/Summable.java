package com.shuffle.monad;

import java.util.concurrent.ExecutionException;

/**
 * Created by DanielKrawisz on 3/2/16.
 */
public interface Summable<X> {
    public interface SummableElement<X> {
        X value();

        SummableElement<X> plus(SummableElement<X> x);
    }

    public abstract class Zero<X> implements SummableElement<X> {

        @Override
        public SummableElement<X> plus(SummableElement<X> x) {
            return x;
        }
    }

    public Zero<X> zero();

    public SummableElement<X> make(X x);
}
