/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;

import com.shuffle.chan.Send;

import java.io.Serializable;

/**
 * A representation of a remote peer to which we can open communication channels.
 *
 * Created by Daniel Krawisz on 1/25/16.
 */
public interface Peer<Identity, Message extends Serializable> {

    Identity identity();

    // Returns null if there is a session already open.
    Session<Identity, Message> openSession(Send<Message> send) throws InterruptedException;

    // Close any open sessions for this peer.
    void close() throws InterruptedException;
}
