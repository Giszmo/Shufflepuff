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

/**
 * A summable future that is already done.
 *
 * Created by Daniel Krawisz on 3/2/16.
 */
public class DoneSummableFuture<X> extends SummableFutureAbstract<X> {
    private final X x;

    public DoneSummableFuture(X x) {
        this.x = x;
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
    public X get(long l, TimeUnit timeUnit) {
        return x;
    }

    @Override
    public X value() {
        return x;
    }
}
