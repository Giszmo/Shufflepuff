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

/**
 * TODO
 *
 * Created by Daniel Krawisz on 1/31/16.
 */

public class WebsocketChannel implements Channel<URI, Bytestring>{

    private class Peers {

        private final Map<URI, WebsocketPeer> peers = new HashMap<URI, WebsocketPeer>();

        public synchronized WebsocketPeer get(URI identity) {
            WebsocketPeer peer = peers.get(identity);
            if (peer == null) {
                peer = peers.put(identity, new WebsocketPeer(identity));
            }
            return peer;
        }
    }

    Peers peers = new Peers();

    class OpenSessions {

        private Map<URI, WebsocketPeer.WebsocketSession> openSessions = new HashMap<>();

        public synchronized WebsocketPeer.WebsocketSession putNewSession(URI identity, WebsocketPeer peer) {
            WebsocketPeer.WebsocketSession openSession = openSessions.get(identity);
            if (openSession != null) {
                if (!openSession.closed()) { //? there is no socket.isConnected() equivalent for WebsocketSession
                    return null;
                }

                openSessions.remove(identity);
            }

            //WebsocketPeer.WebsocketSession session = peer.newSession();
            WebsocketClientEndpoint clientEndPoint = new WebsocketClientEndpoint(new URI("wss://echo.websocket.org"));
            WebsocketPeer.WebsocketSession session = new WebsocketPeer.WebsocketSession(clientEndPoint.newSession());

            return openSessions.put(identity, session);
        }

        public synchronized WebsocketPeer.WebsocketSession putOpenSessions(URI identity, Session client) {
            WebsocketPeer.WebsocketSession openSession = openSessions.get(identity);
            if (openSession != null) {
                if (!openSession.closed()) { //? there is no socket.isConnected() equivalent for WebsocketSession
                    return null;
                }

                openSessions.remove(identity);
            }

            WebsocketPeer peer;
            try {
                peer = peers.get(identity).setSession(client);
            } catch (IOException e) {
                return null;
            }

            return openSessions.put(identity, peer.currentSession);
        }

        public WebsocketPeer.WebsocketSession get(URI identity) {
            return openSessions.get(identity);
        }

        public synchronized WebsocketPeer.WebsocketSession remove(URI identity) {
            return openSessions.remove(identity);
        }

    }

    OpenSessions openSessions = null;

    public class WebsocketPeer extends Peer<URI, Bytestring> {

        List<Session<URI, Bytestring>> history;

        WebsocketSession currentSession;

        public WebsocketPeer(URI identity) {
            super(identity);
        }

        public WebsocketPeer(URI identity, WebsocketSession session) {
            super(identity);
            this.currentSession = session;
        }

        private WebsocketPeer setSession(Session session) throws IOException {
            currentSession = new WebsocketSession(session);
            return this;
        }

        @Override
        public synchronized Session<URI, Bytestring> openSession(Receiver<Bytestring> receiver) { // ignore receiver NOT
            if (currentSession != null) {
                return null;
            }

            WebsocketSession session = openSessions.putNewSession(identity(), this);

            if (session == null) {
                return null;
            }

            return session;
        }


        public class WebsocketSession implements Session<URI, Bytestring> {
            Session session;

            public WebsocketSession (Session session) throws IOException {
                this.session = session;
            }

            @Override
            public boolean send(Bytestring message) {
                try {
                    session.getBasicRemote().sendText(message); // can't sendText a Bytestring
                } catch (IOException e) {
                    return false;
                }
                return true;
            }

            @Override
            public synchronized void close() {
                try {
                    session.close();
                } catch (IOException e) {

                }
                session = null;
                WebsocketPeer.this.currentSession = null;
                openSessions.remove(WebsocketPeer.this.identity());
            }

            @Override
            public boolean closed() {
                return session == null || !session.isOpen();
            }

            @Override
            public Peer<URI, Bytestring> peer() {
                return WebsocketPeer.this;
            }

        }

    }

    private boolean running = false; // not necessary?

    public WebsocketChannel() {

    }

    private class WebsocketConnection implements Connection<URI, Bytestring> {

        @Override

        public void close() { //  not necessary, we don't have ServerSocket listeners?
            running = false;
        }
    }

    @Override
    public Connection<URI, Bytestring> open(Listener<URI, Bytestring> listener) {
        return null;
    }

    @Override
    public Peer<URI, Bytestring> getPeer(URI you) {
        return null;
    }
}
