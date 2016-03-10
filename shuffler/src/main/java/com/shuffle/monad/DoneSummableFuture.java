package com.shuffle.monad;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A summable future that is already done.
 *
 * Created by Daniel Krawisz on 3/2/16.
 */
public class DoneSummableFuture<X> extends SummableFutureAbstract<X> {
    final X x;

    public DoneSummableFuture(X x) {
        this.x = x;
    }

    @Override
    public Summable.SummableElement<X> getSummable() throws InterruptedException, ExecutionException {
        return this;
    }

    @Override
    public Summable.SummableElement<X> getSummable(long l, TimeUnit t) throws InterruptedException, ExecutionException {
        return this;
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
    public X get() throws InterruptedException, ExecutionException {
        return x;
    }

    @Override
    public X get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return x;
    }

    @Override
    public X value() {
        return x;
    }
}
