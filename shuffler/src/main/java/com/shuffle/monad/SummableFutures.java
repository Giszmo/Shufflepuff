/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.monad;

/**
 * Created by Daniel Krawisz on 3/2/16.
 */
public class SummableFutures<X> implements Summable<X> {

    @Override
    public Zero<X> zero() {
        return new SummableFutureZero<>(this);
    }

    @Override
    public SummableElement<X> make(X x) {
        return new DoneSummableFuture<>(x);
    }

}
