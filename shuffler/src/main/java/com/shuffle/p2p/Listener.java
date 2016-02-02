package com.shuffle.p2p;

/**
 * Created by Daniel Krawisz on 1/25/16.
 */
public interface Listener<Identity, Message, Token> extends Runnable{
    // Get a new message receiver for a peer.
    Receiver<Message> receiver();

    // Message to call when a new tcppeer is found.
    void newPeer(Peer<Identity, Message, Token> peer);
}
