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
 * A thing that is both summable and a future.
 *
 * Created by Daniel Krawisz on 3/2/16.
 */
public interface SummableFuture<X> extends Summable.SummableElement<X>, Future<X> {
    SummableFuture<X> plus(SummableFuture<X> x);

    Summable.SummableElement<X> getSummable() throws InterruptedException, ExecutionException;

    Summable.SummableElement<X> getSummable(long l, TimeUnit t)
            throws InterruptedException, ExecutionException, TimeoutException;
}
