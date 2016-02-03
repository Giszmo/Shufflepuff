package com.shuffle.player;

import com.shuffle.p2p.Bytestring;
import com.shuffle.p2p.Channel;
import com.shuffle.p2p.Listener;
import com.shuffle.p2p.Peer;
import com.shuffle.p2p.TCPChannel;
import com.shuffle.p2p.WebsocketChannel;

import java.io.IOException;

/**
 * TODO
 *
 * Created by Daniel Krawisz on 1/31/16.
 */
public class Multiplexer implements Channel<Identity, Bytestring> {
    TCPChannel tcp;
    WebsocketChannel websocket;

    @Override
    public void listen(Listener<Identity, Bytestring> listener) throws IOException {

    }

    @Override
    public Peer<Identity, Bytestring> getPeer(Identity you) {
        return null;
    }
}
