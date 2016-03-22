/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.monad;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Two summable futures whose results are to be added together.
 *
 * Created by Daniel Krawisz on 3/2/16.
 */
public class PlusSummableFuture<X> extends SummableFutureAbstract<X> {
    final SummableFuture<X> fore, aft;

    public PlusSummableFuture(SummableFuture<X> fore, SummableFuture<X> aft) {
        if (fore == null || aft == null) {
            throw new NullPointerException();
        }
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

    private Summable.SummableElement<X> getSummable(Summable.SummableElement<X> foreResult, Summable.SummableElement<X> aftResult) {
        if (foreResult == null) {
            return aftResult; // Could still be null, but let the next guy deal with it.
        }

        if (aftResult == null) {
            return foreResult;
        }

        return foreResult.plus(aftResult);
    }

    @Override
    public Summable.SummableElement<X> getSummable() throws InterruptedException, ExecutionException {
        return getSummable(fore.getSummable(), aft.getSummable());
    }

    @Override
    public Summable.SummableElement<X> getSummable(long l, TimeUnit t) throws InterruptedException, ExecutionException, TimeoutException {
        // TODO: this is not correct; need to determine the correct values for time units.
        Summable.SummableElement<X> foreResult = fore.getSummable(l, t);
        Summable.SummableElement<X> aftResult = aft.getSummable(l, t);
        return getSummable(foreResult, aftResult);
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
        Summable.SummableElement<X> summable = getSummable();
        if (summable == null) {
            return null;
        }
        return summable.value();
    }

    @Override
    public X get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        Summable.SummableElement<X> summable = getSummable(l, timeUnit);
        if (summable == null) {
            return null;
        }
        return summable.value();
    }
}
