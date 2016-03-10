package com.shuffle.p2p;

import com.shuffle.p2p.Channel;
import com.shuffle.p2p.Listener;
import com.shuffle.p2p.Peer;
import com.shuffle.player.Connect;

import java.io.IOException;

/**
 * TODO
 *
 * Created by Daniel Krawisz on 1/26/16.
 */
public class Mediator<Identity, Message> implements Channel<Identity, Message> {
    @Override
    public Connection<Identity, Message> open(Listener<Identity, Message> listener) {
        return null;
    }

    @Override
    public Peer<Identity, Message> getPeer(Identity you) {
        return null;
    }
}
