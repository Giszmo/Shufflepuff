package com.shuffle.p2p;

import com.shuffle.p2p.Channel;
import com.shuffle.p2p.Listener;
import com.shuffle.p2p.Peer;

import java.io.IOException;

/**
 * Created by Daniel Krawisz on 1/26/16.
 */
public class MediatedChannel<Identity, Message, Token> implements Channel<Identity, Message> {
    @Override
    public void listen(Listener<Identity, Message> listener) throws IOException {

    }

    @Override
    public Peer<Identity, Message> getPeer(Identity you) {
        return null;
    }
}
