package com.shuffle.p2p;

import java.io.IOException;

/**
 * Created by cosmos on 1/25/16.
 */
public interface Peer<Identity, Message, Token> {
    Identity getIdentity();

    // Returns null if there is a session already open.
    Session<Message, Token> openSession();
}
