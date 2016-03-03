package com.shuffle.monad;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by Daniel Krawisz on 3/2/16.
 */
public class PlusSummableFuture<X> extends SummableFutureAbstract<X> {
    final SummableFuture<X> fore, aft;

    public PlusSummableFuture(SummableFuture<X> fore, SummableFuture<X> aft) {
        this.fore = fore;
        this.aft = aft;
    }

    @Override
    public boolean cancel(boolean b) {
        boolean foreCancelled = fore.isCancelled();
        boolean aftCancelled = aft.isCancelled();
        boolean foreCancel = fore.cancel(b);
        boolean aftCancel = aft.cancel(b);

        if (foreCancelled && aftCancelled) {
            return false;
        }

        if (!(foreCancelled || aftCancelled)) {
            return foreCancel && aftCancel;
        }

        if (foreCancelled) {
            return aftCancel;
        }

        return foreCancel;
    }

    @Override
    public boolean isCancelled() {
        return fore.isCancelled() && aft.isCancelled();
    }

    @Override
    public boolean isDone() {
        return fore.isDone() && aft.isDone();
    }

    @Override
    public Summable.SummableElement<X> getSummable() throws InterruptedException, ExecutionException {
        return fore.getSummable().plus(aft.getSummable());
    }

    @Override
    public Summable.SummableElement<X> getSummable(long l, TimeUnit t) throws InterruptedException, ExecutionException, TimeoutException {
        // TODO: this is not correct.
        return fore.getSummable(l, t).plus(aft.getSummable(l, t));
    }

    @Override
    public X value() {
        try {
            return get();
        } catch (ExecutionException | InterruptedException e) {
            return null;
        }
    }

    @Override
    public X get() throws ExecutionException, InterruptedException {
        return getSummable().value();
    }

    @Override
    public X get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return getSummable(l, timeUnit).value();
    }
}
