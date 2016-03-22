/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.monad;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by Daniel Krawisz on 3/2/16.
 */
public class SummableMaps<X, Y> implements Summable<Map<X,Y>> {

    @Override
    public Summable.Zero<Map<X, Y>> zero() {
        return new Zero<X, Y>();
    }

    @Override
    public SummableElement<Map<X, Y>> make(Map<X, Y> xyMap) {
        return null;
    }

    public static class Zero<X, Y> extends Summable.Zero<Map<X, Y>> {

        @Override
        public Map<X, Y> value() {
            return new HashMap<X, Y>();
        }
    }

}
