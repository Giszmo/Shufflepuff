package com.shuffle.p2p;

import com.shuffle.p2p.Channel;
import com.shuffle.p2p.Listener;
import com.shuffle.p2p.Peer;

import java.io.IOException;

/**
 * Created by Daniel Krawisz on 1/26/16.
 */
public class MediatedChannel<Identity, Message, Token> implements Channel<Identity, Message, Token> {
    @Override
    public void listen(Listener<Identity, Message, Token> listener) throws IOException {

    }

    @Override
    public Peer<Identity, Message, Token> getPeer(Identity you) {
        return null;
    }
}
