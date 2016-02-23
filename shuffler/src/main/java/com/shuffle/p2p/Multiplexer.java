package com.shuffle.p2p;

import com.shuffle.p2p.Bytestring;
import com.shuffle.p2p.Channel;
import com.shuffle.p2p.Connection;
import com.shuffle.p2p.Listener;
import com.shuffle.p2p.Peer;
import com.shuffle.p2p.TCPChannel;
import com.shuffle.p2p.WebsocketChannel;
import com.shuffle.player.Identity;

import java.io.IOException;

/**
 * TODO
 *
 * Created by Daniel Krawisz on 1/31/16.
 */
public class Multiplexer implements Channel<Identity, Bytestring> {

    @Override
    public Connection<Identity, Bytestring> open(Listener<Identity, Bytestring> listener) throws IOException {
        return null;
    }

    @Override
    public Peer<Identity, Bytestring> getPeer(Identity you) {
        return null;
    }
}
