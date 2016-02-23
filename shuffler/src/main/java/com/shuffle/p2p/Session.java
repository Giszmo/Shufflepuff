package com.shuffle.p2p;

import java.io.IOException;

/**
 * The session is an open means of communication between two peers.
 *
 * Created by Daniel Krawisz on 1/25/16.
 */
public interface Session<Identity, Message> {
    // Send a message.
    boolean send(Message message) ;
    
    // Close the session.
    void close();

    // The peer corresponding to this session.
    Peer<Identity, Message> peer();
}
