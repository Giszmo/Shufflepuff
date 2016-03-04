package com.shuffle.p2p;

import java.io.IOException;

/**
 * A representation of a remote peer to which we can open communication channels.
 *
 * Created by Daniel Krawisz on 1/25/16.
 */
public abstract class Peer<Identity, Message> {
    private final Identity you;
    protected Session<Identity, Message> currentSession = null;

    protected Peer(Identity you) {
        this.you = you;
    }

    public final Identity identity() {
        return you;
    }

    // Returns null if there is a session already open.
    public abstract Session<Identity, Message> openSession(Receiver<Message> receiver) throws InterruptedException;

    // Whether there is an open session to this peer.
    public final boolean open() {
        return currentSession != null && !currentSession.closed();
    }

    // Close any open sessions for this peer.
    public final void close() {
        if (currentSession != null) {
            currentSession.close();
        }
        currentSession = null;
    }
}
