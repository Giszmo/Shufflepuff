/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.monad;

/**
 * Represents a type that is either X or Y.
 *
 * Created by Daniel Krawisz on 3/18/16.
 */
public class Either<X, Y> {
    public final X first;
    public final Y second;

    public Either(X first, Y second) {
        if (first == null) {
            if (second == null) {
                throw new NullPointerException();
            }
        } else if (second != null) {
            throw new IllegalArgumentException();
        }

        this.first = first;
        this.second = second;
    }
}
