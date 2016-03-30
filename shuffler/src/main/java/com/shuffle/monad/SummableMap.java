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

/**
 * Two maps can be summed by creating a third map having the values of both.
 *
 * Created by Daniel Krawisz on 3/2/16.
 */
public class SummableMap<X, Y> implements Summable.SummableElement<Map<X, Y>> {
    Map<X, Y> map;

    public SummableMap(X x, Y y) {
        map = new HashMap<>();
        map.put(x, y);
    }

    public SummableMap(Map<X, Y> m) {
        map = m;
    }

    @Override
    public Map<X, Y> value() {
        return map;
    }

    @Override
    public Summable.SummableElement<Map<X, Y>> plus(Summable.SummableElement<Map<X, Y>> x) {
        if (x == null) {
            return this;
        }

        if (!(x instanceof SummableMap)) {
            return null;
        }

        SummableMap<X, Y> w = (SummableMap<X, Y>) x;

        Map<X, Y> m = new HashMap<>();
        m.putAll(map);
        m.putAll(w.value());
        return new SummableMap<>(m);
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
