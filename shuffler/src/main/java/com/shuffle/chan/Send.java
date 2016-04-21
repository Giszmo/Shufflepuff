/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.chan;

/**
 * A chan class made to work similar to the chan type in golang.
 *
 * X should be an immutable object.
 *
 * Created by Daniel Krawisz on 3/3/16.
 */
public interface Send<X> {
    // Send an X.
    boolean send(X x) throws InterruptedException;

    // Close the chan.
    void close() throws InterruptedException;
}
