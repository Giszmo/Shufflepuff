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
 * An implementation of Peer which is good for implementations which do not contain
 * other Peer objects, ie, they are fundamental.
 *
 * Created by Daniel Krawisz on 3/18/16.
 */
public abstract class FundamentalPeer<Identity, Message> implements Peer<Identity, Message> {
    private final Identity you;
    protected Session<Identity, Message> currentSession = null;

    protected FundamentalPeer(Identity you) {
        this.you = you;
    }

    public final Identity identity() {
        return you;
    }

    // Returns null if there is a session already open.
    public abstract Session<Identity, Message> openSession(Send<Message> send)
            throws InterruptedException;

    // Whether there is an open session to this peer.
    public final boolean open() throws InterruptedException {
        return currentSession != null && !currentSession.closed();
    }

    // Close any open sessions for this peer.
    public final void close() throws InterruptedException {
        if (currentSession != null) {
            currentSession.close();
        }
        currentSession = null;
    }
}
