/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;

import org.glassfish.tyrus.core.TyrusSession;
import org.glassfish.tyrus.server.Server;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.MessageHandler;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerEndpoint;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Created by Eugene Siegel on 4/28/16.
 */

public class TestWebsocketChannel {

    Connection<InetAddress, Bytestring> conn;
    WebsocketTestClient.WebsocketPeer.WebsocketSession clientSession;
    WebsocketTestServer.WebsocketPeer.WebsocketSession serverSession;
    WebsocketTestServer server;
    WebsocketTestClient client;

    public class WebsocketTestClient implements Channel<URI, Bytestring> {

        String globalMessage;

        @ClientEndpoint
        public class WebsocketClientEndpoint {

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
                this.userSession = userSession;
            }

            @OnMessage
            public void onMessage(byte[] message, Session userSession) {
                globalMessage = new String(message);
                // TODO receiver?
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
                    peer = new WebsocketPeer(identity);
                    peers.put(identity, peer);
                }
                return peer;
            }
        }

        final Peers peers = new Peers();

        class OpenSessions {

            private Map<URI, WebsocketPeer.WebsocketSession> openSessions = new ConcurrentHashMap<>();

            public synchronized WebsocketPeer.WebsocketSession putNewSession(URI identity, WebsocketPeer peer) {

                WebsocketPeer.WebsocketSession openSession = openSessions.get(identity);
                if (openSession != null) {
                    if (openSession.session.isOpen()) {
                        return null;
                    }

                    openSessions.remove(identity);
                }

                WebsocketPeer.WebsocketSession session = null;
                try {
                    session = peer.newSession();
                } catch (DeploymentException e) {
                    return null;
                }

                openSessions.put(identity, session);

                return session;
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

            private WebsocketPeer setSession(Session session) throws IOException {
                currentSession = new WebsocketSession(session);
                return this;
            }

            WebsocketSession newSession() throws DeploymentException {
                URI identity = identity();
                try {
                    currentSession = new WebsocketPeer(identity).new WebsocketSession(new WebsocketClientEndpoint(identity).newSession());
                    return currentSession;
                } catch (IOException e) {
                    return null;
                }
            }

            @Override
            public synchronized com.shuffle.p2p.Session<URI, Bytestring> openSession(final Receiver<Bytestring> receiver) {

                synchronized (lock) { }

                if (openSessions == null) {
                    return null;
                }

                if (currentSession != null) {
                    return null;
                }

                final WebsocketSession session = openSessions.putNewSession(identity(), this);
                if (session == null) {
                    return null;
                }

                session.session.addMessageHandler(new MessageHandler.Whole<byte[]>() {
                    public void onMessage(byte[] message) {
                        try {
                            receiver.receive(new Bytestring(message));
                        } catch (InterruptedException e) {
                            session.close();
                        }
                    }
                });

                return session;
            }

            public class WebsocketSession implements com.shuffle.p2p.Session<URI, Bytestring> {
                Session session;

                public WebsocketSession(Session session) throws IOException {
                    this.session = session;
                }

                @Override
                public synchronized boolean send(Bytestring message) {

                    synchronized (lock) { }

                    if (!session.isOpen()) {
                        return false;
                    }

                    try {
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

        private boolean running = false;
        private final Object lock = new Object();

        public WebsocketTestClient() {

        }

        private class WebsocketConnection implements Connection<URI, Bytestring> {

            @Override
            public URI identity() {
                // TODO
                throw new NotImplementedException();
            }

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

    public static class WebsocketTestServer implements Channel<InetAddress, Bytestring> {

        static Listener<InetAddress, Bytestring> globalListener = null;
        static Receiver<Bytestring> globalReceiver = null;
        static String globalMessage = null;

        @ServerEndpoint("/")
        public static class WebsocketServerEndpoint {

            Session userSession;

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

                WebsocketPeer.WebsocketSession session = openSessions.putOpenSession(identity, this.userSession);
                globalReceiver = globalListener.newSession(session);
            }

            @OnMessage
            public void onMessage(byte[] message, Session userSession) throws InterruptedException {
                globalMessage = new String(message);
                Bytestring bytestring = new Bytestring(message);
                globalReceiver.receive(bytestring);
            }

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

            private Map<InetAddress, WebsocketPeer.WebsocketSession> openSessions = new ConcurrentHashMap<>();

            public synchronized WebsocketPeer.WebsocketSession putOpenSession(InetAddress identity, Session session) {
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

        private static OpenSessions openSessions = null;

        public class WebsocketPeer extends FundamentalPeer<InetAddress, Bytestring> {

            WebsocketSession currentSession;

            public WebsocketPeer(InetAddress identity) {
                super(identity);
            }

            private WebsocketPeer setSession(Session session) throws IOException {
                currentSession = new WebsocketSession(session);
                return this;
            }

            @Override
            public synchronized com.shuffle.p2p.Session<InetAddress, Bytestring> openSession(final Receiver<Bytestring> receiver) {
                return null;
            }

            public class WebsocketSession implements com.shuffle.p2p.Session<InetAddress, Bytestring> {

                Session session;

                public WebsocketSession(Session session) throws IOException {
                    this.session = session;
                }

                @Override
                public synchronized boolean send(Bytestring message) {

                    synchronized (lock) { }

                    if (!session.isOpen()) {
                        return false;
                    }

                    try {
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
        private Server server;
        private boolean running = false;
        private final Object lock = new Object();

        public WebsocketTestServer(int port, String hostName, InetAddress me) {

            if (me == null) {
                throw new NullPointerException();
            }

            this.port = port;
            this.hostName = hostName;
            this.me = me;
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

    @Before
    public void setup() throws UnknownHostException, URISyntaxException, DeploymentException, InterruptedException {

        server = new WebsocketTestServer(8080, "localhost", InetAddress.getLocalHost());

        Listener<InetAddress, Bytestring> serverListener = new Listener<InetAddress, Bytestring>() {
            @Override
            public Receiver<Bytestring> newSession(com.shuffle.p2p.Session<InetAddress, Bytestring> session) throws InterruptedException {
                return new Receiver<Bytestring>() {
                    @Override
                    public void receive(Bytestring bytestring) throws InterruptedException {
                        return;
                    }
                };
            }
        };

        Listener<URI, Bytestring> clientListener = new Listener<URI, Bytestring>() {
            @Override
            public Receiver<Bytestring> newSession(com.shuffle.p2p.Session<URI, Bytestring> session) throws InterruptedException {
                return new Receiver<Bytestring>() {
                    @Override
                    public void receive(Bytestring bytestring) throws InterruptedException {
                        return;
                    }
                };
            }
        };

        conn = server.open(serverListener);

        client = new WebsocketTestClient();
        client.open(clientListener);
        WebsocketTestClient.WebsocketPeer peer = client.new WebsocketPeer(new URI("ws://localhost:8080"));
        clientSession = peer.newSession();
        String message = "Shufflepuff";
        Bytestring bytestring = new Bytestring(message.getBytes());
        clientSession.send(bytestring);

        Thread.sleep(2000);

        String message2 = "Slytherin";
        serverSession = server.openSessions.get(InetAddress.getByName("127.0.0.1"));
        Bytestring bytestring2 = new Bytestring(message2.getBytes());
        serverSession.send(bytestring2);

        Thread.sleep(2000);
        Assert.assertEquals(message2, client.globalMessage);
        Assert.assertEquals(message, server.globalMessage);

        //Assert.assertNotNull(server.globalListener);
        //Assert.assertNotNull(server.globalReceiver);

    }

    @After
    public void shutdown() {
        clientSession.close();
        serverSession.close();
        conn.close();
        Assert.assertTrue(clientSession.closed());
        Assert.assertTrue(serverSession.closed());
    }

    @Test
    public void testOnAndOff() {

    }

}
