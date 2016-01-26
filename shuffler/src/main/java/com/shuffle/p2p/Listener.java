package com.shuffle.p2p;

/**
 * Created by cosmos on 1/25/16.
 */
public interface Listener<Identity, Message, Token> {
    // Message to call when a new tcppeer is found.
    void newPeer(Peer<Identity, Message, Token> peer);
}
