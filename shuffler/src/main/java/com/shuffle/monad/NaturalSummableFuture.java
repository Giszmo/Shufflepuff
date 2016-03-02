package com.shuffle.monad;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by Daniel Krawisz on 3/2/16.
 */
public class NaturalSummableFuture<X> extends SummableFutureAbstract<X> {
    final Future<Summable.SummableElement<X>> f;

    public NaturalSummableFuture(Future<Summable.SummableElement<X>> f) {
        this.f = f;
    }

    @Override
    public Summable.SummableElement<X> getSummable() throws InterruptedException, ExecutionException {
        return f.get();
    }

    @Override
    public Summable.SummableElement<X> getSummable(long l, TimeUnit t) throws InterruptedException, ExecutionException, TimeoutException {
        return f.get(l, t);
    }

    @Override
    public boolean cancel(boolean b) {
        return f.cancel(b);
    }

    @Override
    public boolean isCancelled() {
        return f.isCancelled();
    }

    @Override
    public boolean isDone() {
        return f.isDone();
    }

    @Override
    public X get() throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public X get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    @Override
    public X value() {
        return null;
    }
}
