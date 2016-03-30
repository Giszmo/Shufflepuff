/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.monad;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

/**
 * Zero element for summable futures. (In other words, you can add this to any summable future
 * without changanging it.
 *
 * Created by Daniel Krawisz on 3/2/16.
 */
public class SummableFutureZero<X> extends Summable.Zero<X> implements SummableFuture<X> {
    private final X zero;

    public SummableFutureZero(Summable<X> s) {
        this.zero = s.zero().value();
    }

    @Override
    public boolean cancel(boolean b) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public X value() {
        return zero;
    }

    @Override
    public X get() throws InterruptedException, ExecutionException {
        return zero;
    }

    @Override
    public X get(long l, TimeUnit timeUnit)
            throws InterruptedException, ExecutionException, TimeoutException {

        return zero;
    }

    @Override
    public SummableFuture<X> plus(SummableFuture<X> x) {
        return x;
    }

    @Override
    public Summable.SummableElement<X> getSummable()
            throws InterruptedException, ExecutionException {

        return this;
    }

    @Override
    public Summable.SummableElement<X> getSummable(long l, TimeUnit t)
            throws InterruptedException, ExecutionException {

        return this;
    }
}
