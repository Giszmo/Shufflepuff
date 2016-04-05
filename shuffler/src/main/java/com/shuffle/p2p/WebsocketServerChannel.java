/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.Session;

/**
 * Created by Eugene Siegel on 4/1/16.
 */

public class WebsocketServerChannel implements Channel<URI, Bytestring> {

    private class Peers {

        private final Map<URI, WebsocketPeer> peers = new HashMap<>();

        public synchronized WebsocketPeer get(URI identity) {
            WebsocketPeer peer = peers.get(identity);
            if (peer == null) {
                peer = peers.put(identity, new WebsocketPeer(identity));
            }
            return peer;
        }

    }

    final Peers peers = new Peers();

    public class WebsocketPeer extends FundamentalPeer<URI, Bytestring> {

        WebsocketSession currentSession;

        public WebsocketPeer(URI identity) { super(identity); }

        // this doesn't open a session?
        private WebsocketPeer setSession(javax.websocket.Session session) throws IOException {
            currentSession = new WebsocketSession(session);
            return this;
        }

        // newSession()

        // no openSession() ?
        // openSession does receive though...

        @Override
        public synchronized com.shuffle.p2p.Session<URI, Bytestring> openSession(
                final Receiver<Bytestring> receiver) {
            return null;
        }


        public class WebsocketSession implements com.shuffle.p2p.Session<URI, Bytestring> {
            javax.websocket.Session session;

            // is this constructor necessary if we can't open sessions?
            public WebsocketSession(javax.websocket.Session session) throws IOException {
                this.session = session;
            }

            @Override
            // how can I send if I can't open?
            public synchronized boolean send(Bytestring message) {
                // Don't allow sending messages while we're opening or closing the channel.
                synchronized (lock) { }

                if (!session.isOpen()) {
                    return false;
                }

                try {
                    // MUST sendBinary rather than sendText to receive byte[] messages
                    ByteBuffer buf = ByteBuffer.wrap(message.bytes);
                    session.getBasicRemote().sendBinary(buf);
                } catch (IOException e) {
                    return false;
                }
                return true;
            }

            @Override
            public synchronized void close() {
                // how can I close if I can't open?

            }

            @Override
            public synchronized boolean closed() {
                return session == null || !session.isOpen();
            }

            @Override
            public Peer<URI, Bytestring> peer() {
                return WebsocketPeer.this;
            }

        }

    }

    private boolean running = false;
    private final Object lock = new Object();

    public WebsocketServerChannel() {

    }

    private class WebsocketConnection implements Connection<URI, Bytestring> {

        @Override

        // close() method uses openSessions...
        public void close() {

        }

    }


    @Override
    public Connection<URI, Bytestring> open(Listener<URI, Bytestring> listener) {
        return null;
    }

    @Override
    public Peer<URI, Bytestring> getPeer(URI you) {
        return peers.get(you);
    }

}
