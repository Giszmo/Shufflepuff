/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.monad;

import java.util.concurrent.ExecutionException;

/**
 * Created by DanielKrawisz on 3/2/16.
 */
public interface Summable<X> {
    interface SummableElement<X> {
        X value();

        SummableElement<X> plus(SummableElement<X> x);
    }

    abstract class Zero<X> implements SummableElement<X> {

        @Override
        public SummableElement<X> plus(SummableElement<X> x) {
            return x;
        }
    }

    Zero<X> zero();

    SummableElement<X> make(X x);
}
