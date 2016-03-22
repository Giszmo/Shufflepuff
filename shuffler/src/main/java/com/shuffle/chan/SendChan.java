/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.chan;

/**
 * Created by Daniel Krawisz on 3/3/16.
 */
public interface SendChan<X> {
    boolean send(X x);
    void close();
}
