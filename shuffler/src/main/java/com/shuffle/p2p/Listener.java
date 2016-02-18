package com.shuffle.p2p;

/**
 * Functions to be called by a thread that receives new peers.
 *
 * Created by Daniel Krawisz on 1/25/16.
 */
public interface Listener<Identity, Message> {
    // Get a new message receiver for a peer.
    Receiver<Message> receiver();

    // Message to call when a new peer is found.
    void newSession(Session<Identity, Message> session);
}
