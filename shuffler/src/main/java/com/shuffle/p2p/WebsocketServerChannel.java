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
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * Created by Eugene Siegel on 4/1/16.
 */

public class WebsocketServerChannel implements Channel<URI, Bytestring> {

    /**
     *  Necessary class to listen for remote websocket peers
     */

    @ServerEndpoint("") // path or endpoint goes here: wss://localhost:8080/
    private class WebsocketServerEndpoint {

        Session userSession = null;

        @OnOpen
        public void onOpen(Session userSession) {
            this.userSession = userSession;
        }

        @OnClose
        public void onClose(Session userSession, CloseReason reason) {
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

        // The sessions which are currently open.
        private Map<URI, WebsocketPeer.WebsocketSession> openSessions = new ConcurrentHashMap<>();

        // This is for creating a session that was initiated by a remote peer.
        public synchronized WebsocketPeer.WebsocketSession putOpenSession(
                URI identity,
                Session session
        ) {
            WebsocketPeer.WebsocketSession openSession = openSessions.get(identity);
            if (openSession != null) {
                if (openSession.session.isOpen()) {
                    return null;
                }

                openSessions.remove(identity);
            }

            WebsocketPeer peer;
            try {
                peer = peers.get(identity).setSession(session);
            } catch (IOException e) {
                return null;
            }

            return openSessions.put(identity, peer.currentSession);

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

    private OpenSessions openSessions = null;

    public class WebsocketPeer extends FundamentalPeer<URI, Bytestring> {

        WebsocketSession currentSession;

        public WebsocketPeer(URI identity) { super(identity); }

        private WebsocketPeer setSession(javax.websocket.Session session) throws IOException {
            currentSession = new WebsocketSession(session);
            return this;
        }

        @Override
        public synchronized com.shuffle.p2p.Session<URI, Bytestring> openSession(
                final Receiver<Bytestring> receiver) {
            return null;
        }


        public class WebsocketSession implements com.shuffle.p2p.Session<URI, Bytestring> {
            javax.websocket.Session session;

            public WebsocketSession(javax.websocket.Session session) throws IOException {
                this.session = session;
            }

            @Override
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
                try {
                    session.close();
                } catch (IOException e) {

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

    private class WebsocketListener implements Runnable {
        final Listener<URI, Bytestring> listener;

        private WebsocketListener(Listener<URI, Bytestring> listener) {
            this.listener = listener;
        }

        @Override
        public void run() {
            while(true) {
                // New connection found.
                // Not sure if server needs Tyrus Container...
                Session client = server.userSession;
                if (client != null) {

                    URI identity;
                    try {
                        identity = new URI(client.getRequestURI().toString());
                    } catch (URISyntaxException e) {
                        continue; //?
                    }

                    WebsocketPeer.WebsocketSession session = openSessions.putOpenSession(identity, client);

                    if (session == null) {
                        try {
                            client.close();
                        } catch (IOException e) {
                            continue;
                        }
                        continue;
                    }

                    /*
                    Receiver<Bytestring> receiver = listener.newSession(session);

                    if (receiver == null) {
                        continue;
                    }

                    executor.execute();*/
                }
            }
        }
    }

    private WebsocketServerEndpoint server;
    private boolean running = false;
    private final Executor executor;
    private final Object lock = new Object();

    public WebsocketServerChannel(
            Executor executor
    ) {
        if (executor == null) {
            throw new NullPointerException();
        }

        this.executor = executor;
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
        if (listener == null) {
            throw new NullPointerException();
        }

        synchronized (lock) {
            if (running) return null;

            if (server == null) {
                //initialize server & port?? import org.glassfish.tyrus.server.Server??
                server = new WebsocketServerEndpoint();
            }

            running = true;
            openSessions = new OpenSessions();
            executor.execute(new WebsocketListener(listener));
            return new WebsocketConnection();
        }
    }

    @Override
    public Peer<URI, Bytestring> getPeer(URI you) {
        return peers.get(you);
    }

}
