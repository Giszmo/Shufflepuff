/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;

/**
 * Represents an open connection. We can close the connection and that's all.
 *
 * Created by Daniel Krawisz on 2/22/16.
 */
public interface Connection<Identity, Message> {
    Identity identity();

    void close() throws InterruptedException;
}
