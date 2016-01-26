package com.shuffle.p2p;

import com.shuffle.p2p.Channel;
import com.shuffle.p2p.Listener;
import com.shuffle.p2p.Peer;

/**
 * Created by Daniel Krawisz on 1/26/16.
 */
public class MediatedChannel<Identity, Message, Token> implements Channel<Identity, Message, Token> {

    private Peer<Identity, Message, Token> channel;


    @Override
    public boolean listen(Listener<Identity, Message, Token> listener) {
        return false;
    }

    @Override
    public Peer<Identity, Message, Token> getPeer(Identity you) {
        return null;
    }
}
