package com.shuffle.p2p;

import java.io.IOException;

/**
 * A representation of a remote peer to which we can open communication channels.
 *
 * Created by Daniel Krawisz on 1/25/16.
 */
public interface Peer<Identity, Message> {
    Identity identity();

    // Returns null if there is a session already open.
    Session<Identity, Message> openSession(Receiver<Message> receiver);
}
