package com.shuffle.p2p;

import java.io.IOException;

/**
 * Represents an open connection. We can close the connection and that's all.
 *
 * Created by Daniel Krawisz on 2/22/16.
 */
public interface Connection <Identity, Message> {
    void close();
}
