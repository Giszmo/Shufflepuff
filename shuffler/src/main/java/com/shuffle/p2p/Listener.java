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
 * Normally, the Listener is to be implemented by the user of a channel. The purpose
 * of the Listener is to whatever the user wants with a session that is initiated by a
 * remote peer, and to create a Send for the channel to use when a message is received
 * from the remote peer.
 *
 * Created by Daniel Krawisz on 1/25/16.
 */
public interface Listener<Identity, Message> {
    // Message to call when a new peer is found.
    Send<Message> newSession(Session<Identity, Message> session) throws InterruptedException;
}
