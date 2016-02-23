package com.shuffle.p2p;

/**
 * A channel through which connections can be created to other peers.
 *
 * Created by Daniel Krawisz on 1/25/16.
 */

import java.io.IOException;

/**
 * Identity -- the object used to identify a particular tcppeer.
 *
 * Message  -- the object which will be sent back and forth over this channel.
 *
 * Token    -- the information that defines a particular session. For example, for an ecrypted chat
 *             there is a key.
 *
 * Created by Daniel Krawisz on 12/16/15.
 */
public interface Channel<Identity, Message> {
    Peer<Identity, Message> getPeer(Identity you);
    Connection<Identity, Message> open(final Listener<Identity, Message> listener) throws IOException;
}
