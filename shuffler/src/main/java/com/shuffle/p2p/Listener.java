/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;

import com.shuffle.chan.Send;

/**
 * Functions to be called by a thread that receives new peers.
 *
 * Created by Daniel Krawisz on 1/25/16.
 */
public interface Listener<Identity, Message> {
    // Message to call when a new peer is found.
    Send<Message> newSession(Session<Identity, Message> session) throws InterruptedException;
}
