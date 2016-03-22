/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.chan;

import java.util.concurrent.TimeUnit;

/**
 * Created by Daniel Krawisz on 3/3/16.
 */
public interface ReceiveChan<X> {
    X receive() throws InterruptedException;
    X receive(long l, TimeUnit u) throws InterruptedException;
    boolean closed();
}
