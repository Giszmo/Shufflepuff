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
import java.util.Map;
import java.util.List;

import javax.websocket.DeploymentException;
import javax.websocket.MessageHandler;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

/**
 * TODO
 *
 * Created by Daniel Krawisz on 1/31/16.
 */

public class WebsocketChannel implements Channel<URI, Bytestring>{

    @ClientEndpoint
    private class WebsocketClientEndpoint {

        Session userSession = null;
        URI uri;

        public WebsocketClientEndpoint(URI endpointURI) {
            this.uri = endpointURI;
        }

        public Session newSession() throws RuntimeException, DeploymentException, IOException {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            return container.connectToServer(this, this.uri);
        }

        @OnOpen
        public void onOpen(Session userSession) {
            System.out.println("opening websocket");
            this.userSession = userSession;
        }

        @OnClose
        public void onClose(Session userSession, CloseReason reason) {
            System.out.println("closing websocket");
            this.userSession = null;
        }
    }

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

    class OpenSessions {

        private Map<URI, WebsocketPeer.WebsocketSession> openSessions = new HashMap<>(); //ConcurrentHashMap?

        public synchronized WebsocketPeer.WebsocketSession putNewSession(URI identity, WebsocketPeer peer) throws Exception {
            WebsocketPeer.WebsocketSession openSession = openSessions.get(identity);
            if (openSession != null) {
                if (!openSession.closed()) { //? there is no socket.isConnected() equivalent for WebsocketSession
                    return null;
                }

                openSessions.remove(identity);
            }

            WebsocketPeer.WebsocketSession session = peer.newSession();

            return openSessions.put(identity, session);
        }

        public WebsocketPeer.WebsocketSession get(URI identity) {
            return openSessions.get(identity);
        }

        public WebsocketPeer.WebsocketSession remove(URI identity) {
            return openSessions.remove(identity);
        }

        public void closeAll() {
            for (WebsocketPeer.WebsocketSession session : openSessions.values()) {
                session.close();
            }
        }

    }

    OpenSessions openSessions = null;

    public class WebsocketPeer extends FundamentalPeer<URI, Bytestring> {

        List<com.shuffle.p2p.Session<URI, Bytestring>> history;

        WebsocketSession currentSession;

        public WebsocketPeer(URI identity) {
            super(identity);
        }

        public WebsocketPeer(URI identity, WebsocketSession session) {
            super(identity);
            this.currentSession = session;
        }

        private WebsocketPeer setSession(javax.websocket.Session session) throws IOException {
            currentSession = new WebsocketSession(session);
            return this;
        }

        WebsocketPeer.WebsocketSession newSession() throws DeploymentException{
            URI identity = identity();
            try {
                WebsocketClientEndpoint clientEndPoint = new WebsocketClientEndpoint(identity);
                WebsocketPeer.WebsocketSession session = new WebsocketPeer(identity).new WebsocketSession(clientEndPoint.newSession());
                return session;
            } catch(IOException e) {
                return null;
            }
        }

        @Override
        public synchronized com.shuffle.p2p.Session<URI, Bytestring> openSession(final Receiver<Bytestring> receiver) { // had to make it final to call receive in MessageHandler
            synchronized (lock) {}

            if (openSessions == null) {
                return null;
            }

            if (currentSession != null) {
                return null;
            }

            WebsocketSession session;

            try {
                session = openSessions.putNewSession(identity(), this);
                if (session == null) {
                    return null;
                }

                session.session.addMessageHandler(new MessageHandler.Whole<byte[]>() {
                    public void onMessage(byte[] message) {
                        receiver.receive(new Bytestring(message));
                    }
                });

                return session;
            } catch(Exception e) {
                return null;
            }

        }


        public class WebsocketSession implements com.shuffle.p2p.Session<URI, Bytestring> {
            javax.websocket.Session session;

            public WebsocketSession (javax.websocket.Session session) throws IOException {
                this.session = session;
            }

            @Override
            public synchronized boolean send(Bytestring message) {
                synchronized (lock) {}

                if (!session.isOpen()) {
                    return false;
                }

                try {
                    // MUST sendBinary rather than sendText to receive byte[] messages!
                    ByteBuffer buf = ByteBuffer.wrap(message.bytes);
                    session.getBasicRemote().sendBinary(buf);
                } catch (IOException e) {
                    return false;
                }
                return true;
            }

            @Override
            public synchronized void close() {
                try {
                    session.close();
                } catch(IOException e) {

                }
                session = null;
                WebsocketPeer.this.currentSession = null;
                openSessions.remove(WebsocketPeer.this.identity());
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


    public WebsocketChannel() {

    }

    private class WebsocketConnection implements Connection<URI, Bytestring> {

        @Override

        public void close() {
            synchronized (lock) {
                openSessions.closeAll();
                openSessions = null;
                running = false;
            }
        }
    }

    @Override
    public Connection<URI, Bytestring> open(Listener<URI, Bytestring> listener) {
        synchronized (lock) {
            if (running) return null;
            running = true;
            openSessions = new OpenSessions();
            return new WebsocketConnection();
        }
    }

    @Override
    public Peer<URI, Bytestring> getPeer(URI you) {
        return peers.get(you);
    }
}
