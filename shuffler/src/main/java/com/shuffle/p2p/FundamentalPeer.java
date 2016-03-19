package com.shuffle.p2p;

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
