package com.shuffle.p2p;

import com.shuffle.chan.Send;

import java.io.InputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * A manager for a bunch of tcp connections.
 *
 * Created by Daniel Krawisz on 1/25/16.
 */
public class TcpChannel implements Channel<InetSocketAddress, Bytestring> {

    // A message sent over TCP requires a header to tell how long it is.
    public interface Header {
        int headerLength();

        int payloadLength(byte[] header) throws IOException;

        Bytestring makeHeader(int payloadLength) throws IOException;
    }

    private static class DefaultHeader implements Header {

        @Override
        public int headerLength() {
            return 4;
        }

        @Override
        public int payloadLength(byte[] header) throws IOException {
            if (header == null) throw new NullPointerException();

            if (header.length != 4) throw new IOException();

            return ByteBuffer.wrap(header).getInt();
        }

        @Override
        public Bytestring makeHeader(int payloadLength) {
            ByteBuffer dbuf = ByteBuffer.allocate(4);
            dbuf.putInt(payloadLength);
            return new Bytestring(dbuf.array());
        }
    }

    public static Header defaultHeader() {
        return new DefaultHeader();
    }

    // A particular header format that is used for this particular channel.
    private final Header header;

    // Only one object representing each peer is allowed at a time.
    private class Peers {
        private final Map<InetSocketAddress, TcpPeer> peers = new HashMap<>();

        public synchronized TcpPeer get(InetSocketAddress identity) {
            TcpPeer peer = peers.get(identity);
            if (peer == null) {
                peer = new TcpPeer(identity);
                peers.put(identity, peer);
            }

            return peer;
        }
    }

    private final Peers peers = new Peers();

    // A special class used to house synchronized functions regarding the list of open sessions.
    class OpenSessions {

        // The sessions which are currently open.
        private final Map<InetSocketAddress, TcpPeer.TcpSession> openSessions
                = new ConcurrentHashMap<>();

        // We don't want to overwrite a session that already exists, so this is in a synchronized
        // function. This is for creating new sessions.
        public synchronized TcpPeer.TcpSession putNewSession(
                InetSocketAddress identity,
                TcpPeer peer
        ) {
            TcpPeer.TcpSession openSession = openSessions.get(identity);
            if (openSession != null) {
                if (openSession.socket.isConnected()) {
                    return null;
                }

                openSessions.remove(identity);
            }

            TcpPeer.TcpSession session = peer.newSession();

            if (session == null) return null;
            openSessions.put(identity, session);
            return session;
        }

        // This is for creating a session that was initiated by a remote peer.
        public synchronized TcpPeer.TcpSession putOpenSession(
                InetSocketAddress identity,
                Socket client
        ) {
            TcpPeer.TcpSession openSession = openSessions.get(identity);
            if (openSession != null) {
                if (openSession.socket.isConnected()) {
                    return null;
                }

                openSessions.remove(identity);
            }

            TcpPeer peer;
            try {
                peer = peers.get(identity).setSession(client);
            } catch (IOException e) {
                return null;
            }

            openSessions.put(identity, peer.currentSession);

            return peer.currentSession;
        }

        public TcpPeer.TcpSession get(InetSocketAddress identity) {
            return openSessions.get(identity);
        }

        public TcpPeer.TcpSession remove(InetSocketAddress identity) {
            return openSessions.remove(identity);
        }

        public void closeAll() {
            for (TcpPeer.TcpSession session : openSessions.values()) {
                session.close();
            }
        }
    }

    private OpenSessions openSessions = null;

    // Class definition for representation of a particular tcppeer.
    public class TcpPeer extends FundamentalPeer<InetSocketAddress, Bytestring> {

        TcpSession currentSession;

        // Constructor for initiating a connection.
        TcpPeer(InetSocketAddress identity) {
            super(identity);
        }

        // Constructor for a connection that is initiated by a remote peer.
        // TODO it would make more sense to have a socket rather than a session here.
        //
        // TODO figure out what I meant by that previous TODO note and explain it better.
        public TcpPeer(InetSocketAddress identity, TcpSession session) {
            super(identity);
            this.currentSession = session;
        }

        private TcpPeer setSession(Socket socket) throws IOException {
            currentSession = new TcpSession(socket);
            return this;
        }

        TcpSession newSession() {
            InetSocketAddress identity = identity();

            InetAddress address = identity.getAddress();

            if (address == null) {
                return null;
            }

            try {

                Socket socket = new Socket(identity.getAddress(), identity.getPort());

                return new TcpSession(socket);
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        public synchronized Session<InetSocketAddress, Bytestring> openSession(
                Send<Bytestring> send
        ) {
            // Don't allow sessions to be opened when we're opening or closing the channel.
            synchronized (lock) { }

            if (openSessions == null) {
                return null;
            }

            if (currentSession != null) {
                return null;
            }

            TcpSession session = openSessions.putNewSession(identity(), this);

            if (session == null) {
                return null;
            }

            executor.execute(new TcpReceiver(session, send));

            return session;
        }

        // Encapsulates a particular tcp session.
        public class TcpSession implements Session<InetSocketAddress, Bytestring> {
            Socket socket;
            InputStream in;

            TcpSession(Socket socket) throws IOException {
                if (socket == null) {
                    throw new NullPointerException();
                }

                this.socket = socket;
                in = socket.getInputStream();
            }

            @Override
            public synchronized boolean send(Bytestring message) {
                // Don't allow sending messages while we're opening or closing the channel.
                synchronized (lock) { }

                if (socket == null || socket.isClosed()) {
                    return false;
                }

                try {
                    socket.getOutputStream().write(header.makeHeader(message.bytes.length).bytes);
                    socket.getOutputStream().write(message.bytes);
                } catch (IOException e) {
                    // socket should be closed by throwing an exception.
                    socket = null;
                    return false;
                }

                return true;
            }

            @Override
            public synchronized void close() {
                if (socket == null) {
                    return;
                }

                try {
                    socket.close();
                } catch (IOException ignored) {

                }
                socket = null;
                in = null;
                TcpPeer.this.currentSession = null;
                openSessions.remove(TcpPeer.this.identity());
            }

            @Override
            public synchronized boolean closed() {
                return socket == null || socket.isClosed();
            }

            @Override
            public Peer<InetSocketAddress, Bytestring> peer() {
                return TcpPeer.this;
            }
        }
    }

    private class TcpReceiver implements Runnable {
        final TcpPeer.TcpSession session;
        final InputStream in;
        final Send<Bytestring> send;

        private TcpReceiver(TcpPeer.TcpSession session, Send<Bytestring> send) {
            this.session = session;
            this.in = session.in;
            this.send = send;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    byte[] head = new byte[header.headerLength()];
                    in.read(head);

                    byte[] msg = new byte[header.payloadLength(head)];
                    int total = in.read(msg);

                    if (total < msg.length) {
                        session.close();
                        break;
                    }

                    send.send(new Bytestring(msg));

                } catch (IOException | InterruptedException e) {
                    session.close();
                    break;
                }
            }

            try {
                send.close();
            } catch (InterruptedException e) {

            }
        }
    }

    // This contains the function that listens for new tcp connections.
    private class TcpListener implements Runnable {
        final Listener<InetSocketAddress, Bytestring> listener;
        final ServerSocket server;

        private TcpListener(Listener<InetSocketAddress, Bytestring> listener, ServerSocket server) {
            this.listener = listener;
            this.server = server;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    // New connection found.
                    Socket client = server.accept();

                    // Determine the identity of this connection.
                    InetSocketAddress identity =
                            new InetSocketAddress(client.getInetAddress(), client.getPort());

                    TcpPeer.TcpSession session = openSessions.putOpenSession(identity, client);

                    if (session == null) {
                        client.close();
                        continue;
                    }

                    Send<Bytestring> send = listener.newSession(session);

                    if (send == null) {
                        continue;
                    }

                    executor.execute(new TcpReceiver(session, send));
                } catch (IOException | InterruptedException e) {
                    return;
                }
            }
        }
    }

    private final int port;
    private final InetSocketAddress me;

    private ServerSocket server;
    private boolean running = false;
    private final Executor executor;

    private final Object lock = new Object();

    public TcpChannel(
            Header header,
            InetSocketAddress me,
            Executor executor) {

        if (executor == null || header == null || me == null) {
            throw new NullPointerException();
        }

        this.me = me;
        this.header = header;
        this.executor = executor;
        this.port = me.getPort();
    }

    public TcpChannel(InetSocketAddress me, Executor executor) {
        this(defaultHeader(), me, executor);
    }

    private class TcpConnection implements Connection<InetSocketAddress, Bytestring> {

        @Override
        public InetSocketAddress identity() {
            return me;
        }

        @Override
        // TODO should close all connections and stop listening.
        public void close() {
            synchronized (lock) {
                if (server != null) {
                    try {
                        server.close();
                        openSessions.closeAll();
                        openSessions = null;
                    } catch (IOException ignored) {
                        server = null;
                        running = false;
                    }
                }
            }
        }
    }

    @Override
    public Connection<InetSocketAddress, Bytestring> open(
            Listener<InetSocketAddress, Bytestring> listener
    ) {
        if (listener == null) {
            throw new NullPointerException();
        }

        synchronized (lock) {
            if (running) return null;

            if (server == null) {
                try {
                    server = new ServerSocket(port);
                } catch (IOException e) {
                    return null;
                }
            }

            running = true;

            openSessions = new OpenSessions();

            executor.execute(new TcpListener(listener, server));

            return new TcpConnection();
        }
    }

    @Override
    public Peer<InetSocketAddress, Bytestring> getPeer(InetSocketAddress you) {

        if (you.equals(me)) return null;

        return peers.get(you);
    }
}
