package com.shuffle.p2p;

import java.io.IOException;
import java.net.URI;

/**
 * TODO
 *
 * Created by Daniel Krawisz on 1/31/16.
 */
public class WebsocketChannel implements Channel<URI, Bytestring, Void> {
    @Override
    public void listen(Listener<URI, Bytestring, Void> listener) throws IOException {

    }

    @Override
    public Peer<URI, Bytestring, Void> getPeer(URI you) {
        return null;
    }
}
