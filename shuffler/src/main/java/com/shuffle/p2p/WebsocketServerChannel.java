/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.glassfish.tyrus.core.TyrusSession;
import org.glassfish.tyrus.server.Server;

/**
 * Created by Eugene Siegel on 4/1/16.
 */


/**
 *  Starts a Websocket Server and manages connections to this server.
 *  Currently, Websocket Secure (wss) is not supported.
 */

public class WebsocketServerChannel implements Channel<InetAddress, Bytestring> {

    /**
     *  Necessary class to listen for remote websocket peers
     */

    Listener<InetAddress, Bytestring> globalListener = null;

    // path variable here?
    // The below class sets up the WebsocketServer, available at "ws://localhost:port/path"
    // It seems impossible to be able to assign a variable to the @ServerEndpoint annotation,
    // but I will check the Tyrus Glassfish documentation.
    // TODO
    @ServerEndpoint("/")
    private class WebsocketServerEndpoint {

        Session userSession;

        // Callback for when a peer connects to the WebsocketServer.
        @OnOpen
        public void onOpen(Session userSession) throws InterruptedException {
            this.userSession = userSession;
            String clientIp = ((TyrusSession)this.userSession).getRemoteAddr();
            InetAddress identity;
            try {
                identity = InetAddress.getByName(clientIp);
            } catch (UnknownHostException e) {
                try {
                    this.userSession.close();
                } catch (IOException er) {
                    return;
                }
                this.userSession = null;
                return;
            }

            WebsocketPeer.WebsocketSession session = openSessions.putOpenSession(identity,this.userSession);
            globalListener.newSession(session);
        }

        // @OnMessage? Will this receive any messages?

        // Callback for when a peer disconnects from the WebsocketServer
        @OnClose
        public void onClose(Session userSession, CloseReason reason) {

            String sessionIp = ((TyrusSession)this.userSession).getRemoteAddr();
            InetAddress identity;

            try {
                identity = InetAddress.getByName(sessionIp);
            } catch (UnknownHostException er) {
                return;
            }

            try {
                this.userSession.close();
            } catch (IOException e) {
                return;
            }

            openSessions.remove(identity);
            this.userSession = null;
        }

    }

    private class Peers {

        private final Map<InetAddress, WebsocketPeer> peers = new HashMap<>();

        public synchronized WebsocketPeer get(InetAddress identity) {
            WebsocketPeer peer = peers.get(identity);
            if (peer == null) {
                peer = new WebsocketPeer(identity);
                peers.put(identity, peer);
            }

            return peer;
        }
    }

    final Peers peers = new Peers();

    class OpenSessions {

        // The sessions which are currently open.
        private Map<InetAddress, WebsocketPeer.WebsocketSession> openSessions = new ConcurrentHashMap<>();

        // This is for creating a session that was initiated by a remote peer.
        public synchronized WebsocketPeer.WebsocketSession putOpenSession(
                InetAddress identity,
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

            openSessions.put(identity, peer.currentSession);

            return peer.currentSession;

        }

        public WebsocketPeer.WebsocketSession get(InetAddress identity) {
            return openSessions.get(identity);
        }

        public WebsocketPeer.WebsocketSession remove(InetAddress identity) {
            return openSessions.remove(identity);
        }

        public void closeAll() {
            for (WebsocketPeer.WebsocketSession session : openSessions.values()) {
                session.close();
            }
        }

    }

    private OpenSessions openSessions = null;

    public class WebsocketPeer extends FundamentalPeer<InetAddress, Bytestring> {

        WebsocketSession currentSession;

        public WebsocketPeer(InetAddress identity) { super(identity); }

        private WebsocketPeer setSession(javax.websocket.Session session) throws IOException {
            currentSession = new WebsocketSession(session);
            return this;
        }

        @Override
        public synchronized com.shuffle.p2p.Session<InetAddress, Bytestring> openSession(
                final Receiver<Bytestring> receiver) {
            return null;
        }


        public class WebsocketSession implements com.shuffle.p2p.Session<InetAddress, Bytestring> {
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
                    return;
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
            public Peer<InetAddress, Bytestring> peer() {
                return WebsocketPeer.this;
            }

        }

    }

    private final int port;
    private final String hostName;
    private final InetAddress me;
    //private static final String path;
    private Server server;
    private boolean running = false;
    private final Object lock = new Object();

    public WebsocketServerChannel(
            int port,
            String hostName,
            InetAddress me
            //String path
    ) {

        if (me == null) {
            throw new NullPointerException();
        }

        this.port = port;
        this.hostName = hostName;
        this.me = me;
        //this.path = path;
    }

    private class WebsocketConnection implements Connection<InetAddress, Bytestring> {

        public InetAddress identity() {
            return me;
        }

        @Override
        public void close() {
            synchronized (lock) {
                if (server != null) {
                    server.stop();
                    openSessions.closeAll();
                    openSessions = null;
                    running = false;
                    server = null;
                    globalListener = null;
                }
            }
        }
    }


    @Override
    public Connection<InetAddress, Bytestring> open(Listener<InetAddress, Bytestring> listener) {
        if (listener == null) {
            throw new NullPointerException();
        }

        globalListener = listener;

        synchronized (lock) {
            if (running) return null;

            if (server == null) {
                try {
                    // rootPath variable?
                    // initializes and starts the Websocket Server at the specified port
                    server = new Server(hostName, port, "", new HashMap<String, Object>(), WebsocketServerEndpoint.class);
                    server.start();
                } catch (DeploymentException e) {
                    return null;
                }
            }

            running = true;
            openSessions = new OpenSessions();
            return new WebsocketConnection();
        }
    }

    @Override
    public Peer<InetAddress, Bytestring> getPeer(InetAddress you) {

        if (you.equals(me)) return null;

        return peers.get(you);
    }

}
