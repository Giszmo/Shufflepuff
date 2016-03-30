/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.monad;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

/**
 * Transforms a future of a summable into a summable future.
 *
 * Created by Daniel Krawisz on 3/2/16.
 */
public class NaturalSummableFuture<X> extends SummableFutureAbstract<X> {
    final Future<Summable.SummableElement<X>> f;

    public NaturalSummableFuture(Future<Summable.SummableElement<X>> f) {
        this.f = f;
    }

    @Override
    public Summable.SummableElement<X> getSummable()
            throws InterruptedException, ExecutionException {

        return f.get();
    }

    @Override
    public Summable.SummableElement<X> getSummable(long l, TimeUnit t)
            throws InterruptedException, ExecutionException, TimeoutException {

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
        Summable.SummableElement<X> r = getSummable();
        if (r == null) {
            return null;
        }
        return r.value();
    }

    @Override
    public X get(long l, TimeUnit timeUnit)
            throws InterruptedException, ExecutionException, TimeoutException {

        Summable.SummableElement<X> r = getSummable(l, timeUnit);
        if (r == null) {
            return null;
        }
        return r.value();
    }

    @Override
    public X value() {
        try {
            return get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }
}
