/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;

/**
 * A single session that acts as a mediator for many virtual sessions.
 *
 * Created by Daniel Krawisz on 1/26/16.
 */
public class Mediator<Identity, Message> implements Channel<Identity, Message> {
    @Override
    public Connection<Identity, Message> open(Listener<Identity, Message> listener) {
        return null;
    }

    @Override
    public Peer<Identity, Message> getPeer(Identity you) {
        return null;
    }
}
