package com.shuffle.p2p;

import java.io.IOException;
import java.net.URI;

/**
 * TODO
 *
 * Created by Daniel Krawisz on 1/31/16.
 */
public class WebsocketChannel implements Channel<URI, Bytestring> {
    @Override
    public Connection<URI, Bytestring> open(Listener<URI, Bytestring> listener) {
        return null;
    }

    @Override
    public Peer<URI, Bytestring> getPeer(URI you) {
        return null;
    }
}
