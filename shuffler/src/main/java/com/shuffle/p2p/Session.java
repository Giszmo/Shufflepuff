package com.shuffle.p2p;

/**
 * Created by Daniel Krawisz on 1/25/16.
 */
public interface Session<Message> {
    // Send a message.
    boolean send(Message message);

    // Whether a message is ready.
    boolean ready();
    
    // Close the session.
    void close();

    // Whether the session is open.
    boolean isOpen();
}
