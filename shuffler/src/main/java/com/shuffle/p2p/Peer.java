/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;

/**
 * A representation of a remote peer to which we can open communication channels.
 *
 * Created by Daniel Krawisz on 1/25/16.
 */
public interface Peer<Identity, Message> {

    Identity identity();

    // Returns null if there is a session already open.
    Session<Identity, Message> openSession(Receiver<Message> receiver) throws InterruptedException;

    // Whether there is an open session to this peer.
    boolean open();

    // Close any open sessions for this peer.
    void close();
}
