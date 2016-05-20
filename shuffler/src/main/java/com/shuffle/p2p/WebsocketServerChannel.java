/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;

import com.shuffle.chan.Send;

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
import org.glassfish.tyrus.container.grizzly.server.*;


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

    private Listener<InetAddress, Bytestring> globalListener = null;
    // static copy of globalListener.
    private static Listener<InetAddress, Bytestring> staticGlobalListener = null;


    // path variable here?
    // The below class sets up the WebsocketServer, available at "ws://localhost:port/path"
    // It seems impossible to be able to assign a variable to the @ServerEndpoint annotation,
    // but I will check the Tyrus Glassfish documentation.
    // TODO
    @ServerEndpoint("/")
    public static class WebsocketServerEndpoint{

        /**
         *
         * The three variables "listener", "localOpenSessions", and "localPeers" are simply copies
         * of the static variables "staticGlobalListener", "staticOpenSessions", and "staticPeers",
         * respectively.  Since WebsocketServerEndpoint MUST be static if it is an inner class, it
         * can only access outer variables that are also static.  That means if this class needs to
         * access the non-static variables "peers" or "openSessions", it must have a static copy of
         * these variables.  Since these are static copies of non-static variables, any instance of
         * WebsocketServerChannel can alter them.  To ensure that no instance of WebsocketServerChannel
         * alters these static copies while we are using them, we use a synchronized lock in the open()
         * method and then copy these static copies in the WebsocketServerEndpoint constructor.
         *
         */

        Listener<InetAddress, Bytestring> listener;
        OpenSessions localOpenSessions;
        Peers localPeers;
        HashMap<Session, Send> receiveMap;

        public WebsocketServerEndpoint() {
            listener = staticGlobalListener;
            localOpenSessions = staticOpenSessions;
            localPeers = staticPeers;
            receiveMap = new HashMap<>();
        }

        // Callback for when a peer connects to the WebsocketServer.
        @OnOpen
        public void onOpen(Session userSession) throws InterruptedException {
            String clientIp = ((TyrusSession)userSession).getRemoteAddr();
            InetAddress identity;
            try {
                identity = InetAddress.getByName(clientIp);
            } catch (UnknownHostException e) {
                try {
                    userSession.close();
                } catch (IOException er) {
                    return;
                }
                return;
            }

            WebsocketPeer.WebsocketSession session = localOpenSessions.putOpenSession(identity, userSession);
            Send<Bytestring> receiver = listener.newSession(session);
            receiveMap.put(userSession, receiver);
        }

        @OnMessage
        public void onMessage(byte[] message, Session userSession) throws InterruptedException {
            Bytestring bytestring = new Bytestring(message);
            Send<Bytestring> receiver = receiveMap.get(userSession);
            receiver.send(bytestring);
        }

        // Callback for when a peer disconnects from the WebsocketServer
        @OnClose
        public void onClose(Session userSession, CloseReason reason) throws InterruptedException {

            String sessionIp = ((TyrusSession)userSession).getRemoteAddr();
            InetAddress identity;

            try {
                identity = InetAddress.getByName(sessionIp);
            } catch (UnknownHostException er) {
                return;
            }

            try {
                userSession.close();
            } catch (IOException e) {
                return;
            }

            localOpenSessions.remove(identity);
            localPeers.remove(identity);
            receiveMap.get(userSession).close();
            receiveMap.remove(userSession);
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

        public synchronized void remove(InetAddress identity) {
            peers.remove(identity);
        }

    }

    private final Peers peers = new Peers();
    // static copy of peers
    public static Peers staticPeers = null;

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
    // static copy of openSessions
    private static OpenSessions staticOpenSessions = null;

    private class WebsocketPeer extends FundamentalPeer<InetAddress, Bytestring> {

        WebsocketSession currentSession;

        public WebsocketPeer(InetAddress identity) { super(identity); }

        private WebsocketPeer setSession(javax.websocket.Session session) throws IOException {
            currentSession = new WebsocketSession(session);
            return this;
        }

        @Override
        public synchronized com.shuffle.p2p.Session<InetAddress, Bytestring> openSession(
                final Send<Bytestring> receiver) {
            return null;
        }


        private class WebsocketSession implements com.shuffle.p2p.Session<InetAddress, Bytestring> {
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
    // a static lock
    private final static Object staticLock = new Object();

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

    private class WebsocketConnection implements Connection<InetAddress> {
        private boolean closed = false;

        public InetAddress identity() {
            return me;
        }

        @Override
        public void close() {
            if (closed) return;

            synchronized (lock) {
                if (server != null) {
                    closed = true;
                    server.stop();
                    openSessions.closeAll();
                    openSessions = null;
                    running = false;
                    server = null;
                    globalListener = null;
                }
            }
        }

        @Override
        public boolean closed() {
            return closed;
        }
    }


    @Override
    public Connection<InetAddress> open(Listener<InetAddress, Bytestring> listener) {
        if (listener == null) {
            throw new NullPointerException();
        }

        synchronized (lock) {
            if (running) return null;

            /**
             * We must use a static lock to ensure that no other instances of WebsocketServerChannel
             * are altering our static "copy" variables (staticOpenSessions, staticGlobalListener,
             * and staticPeers).
             */
            synchronized (staticLock) {

                /**
                 * We MUST initialize these variables BEFORE server.start() is called.  The start()
                 * method instantiates a WebsocketServerEndpoint class, which REQUIRES the variables
                 * staticOpenSessions, staticGlobalListener, and staticPeers to be initialized.
                 * The variables staticOpenSessions and staticGlobalListener, in turn require
                 * openSessions and globalListener to be initialized.  If these variables are not
                 * initialized here, the possibility of unexpected behavior arises.
                 */

                running = true;
                openSessions = new OpenSessions();
                globalListener = listener;
                staticOpenSessions = openSessions;
                staticGlobalListener = globalListener;
                staticPeers = peers;

                if (server == null) {
                    try {
                        // rootPath variable?
                        // initializes and starts the Websocket Server at the specified hostName and port
                        server = new Server(hostName, port, "", new HashMap<String, Object>(), WebsocketServerEndpoint.class);
                        server.start();
                    } catch (DeploymentException e) {

                        /**
                         * If the server did not start, we can un-initialize the variables that
                         * WebsocketServerEndpoint requires.
                         */

                        running = false;
                        openSessions = null;
                        globalListener = null;
                        staticOpenSessions = null;
                        staticGlobalListener = null;
                        staticPeers = null;
                        return null;
                    }
                }
            }
            return new WebsocketConnection();
        }
    }

    @Override
    public Peer<InetAddress, Bytestring> getPeer(InetAddress you) {

        if (you.equals(me)) return null;

        return peers.get(you);
    }

}
