package com.shuffle.p2p;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * A manager for a bunch of tcp connections.
 *
 * Created by Daniel Krawisz on 1/25/16.
 */
public class TCPChannel implements Channel<InetSocketAddress, Bytestring> {

    // A message sent over TCP requires a header to tell how long it is.
    public interface Header {
        int headerLength();

        int payloadLength(byte[] header);

        com.shuffle.p2p.Bytestring makeHeader(int payloadLength);
    }

    // A particular header format that is used for this particular channel.
    Header header;

    // Only one object representing each peer is allowed at a time.
    private class Peers {
        private final Map<InetSocketAddress, TCPPeer> peers = new HashMap<InetSocketAddress, TCPPeer>();

        public synchronized TCPPeer get(InetSocketAddress identity) {
            TCPPeer peer = peers.get(identity);
            if (peer == null) {
                peer = peers.put(identity, new TCPPeer(identity));
            }
            return peer;
        }
    }

    final Peers peers = new Peers();

    // A special class used to house synchronized functions regarding the list of open sessions.
    class OpenSessions {

        // The sessions which are currently open.
        private Map<InetSocketAddress, TCPPeer.TCPSession> openSessions = new ConcurrentHashMap<>();

        // We don't want to overwrite a session that already exists, so this is in a synchronized
        // function. This is for creatin new sessions.
        public synchronized TCPPeer.TCPSession putNewSession(InetSocketAddress identity, TCPPeer peer) {
            TCPPeer.TCPSession openSession = openSessions.get(identity);
            if (openSession != null) {
                if (openSession.socket.isConnected()) {
                    return null;
                }

                openSessions.remove(identity);
            }

            TCPPeer.TCPSession session = peer.newSession();

            return openSessions.put(identity, session);
        }

        // This is for creating a session that was initiated by a remote peer.
        public synchronized TCPPeer.TCPSession putOpenSession(InetSocketAddress identity, Socket client) {
            TCPPeer.TCPSession openSession = openSessions.get(identity);
            if (openSession != null) {
                if (openSession.socket.isConnected()) {
                    return null;
                }

                openSessions.remove(identity);
            }

            TCPPeer peer;
            try {
                peer = peers.get(identity).setSesssion(client);
            } catch (IOException e) {
                return null;
            }

            return openSessions.put(identity, peer.currentSession);
        }

        public TCPPeer.TCPSession get(InetSocketAddress identity) {
            return openSessions.get(identity);
        }

        public TCPPeer.TCPSession remove(InetSocketAddress identity) {
            return openSessions.remove(identity);
        }

        public void closeAll() {
            for (TCPPeer.TCPSession session : openSessions.values()) {
                session.close();
            }
        }
    }

    OpenSessions openSessions = null;

    // Class definition for representation of a particular tcppeer.
    public class TCPPeer extends Peer<InetSocketAddress, Bytestring>{

        List<Session<InetSocketAddress, Bytestring>> history;

        TCPSession currentSession;

        // Constructor for initiating a connection.
        public TCPPeer(InetSocketAddress identity) {
            super(identity);
        }

        // Constructor for a connection that is initiated by a remote peer.
        // TODO it would make more sense to have a socket rather than a session here.
        public TCPPeer(InetSocketAddress identity, TCPSession session) {
            super(identity);
            this.currentSession = session;
        }

        private TCPPeer setSesssion(Socket socket) throws IOException {
            currentSession = new TCPSession(socket);
            return this;
        }

        TCPPeer.TCPSession newSession() {
            InetSocketAddress identity = identity();

            try {
                return new TCPSession(new Socket(identity.getAddress(), identity.getPort()));
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        public synchronized Session<InetSocketAddress, Bytestring> openSession(Receiver<Bytestring> receiver) {
            // Don't allow sessions to be opened when we're opening or closing the channel.
            synchronized (lock) {}

            if (openSessions == null) {
                return null;
            }

            if (currentSession != null) {
                return null;
            }

            TCPSession session = openSessions.putNewSession(identity(), this);

            if (session == null) {
                return null;
            }

            executor.execute(new TCPReceiver(session, receiver));

            return session;
        }

        // Encapsulates a particular tcp session.
        public class TCPSession implements Session<InetSocketAddress, Bytestring> {
            Socket socket;
            InputStream in;

            public TCPSession(Socket socket) throws IOException {
                this.socket = socket;
                in = socket.getInputStream();
            }

            @Override
            public synchronized boolean send(Bytestring message) {
                // Don't allow sending messages while we're opening or closing the channel.
                synchronized (lock) {}

                if (socket.isClosed()) {
                    return false;
                }

                try {
                    socket.getOutputStream().write(header.makeHeader(message.bytes.length).bytes);
                    socket.getOutputStream().write(message.bytes);
                } catch (IOException e) {
                    return false;
                }

                return true;
            }

            @Override
            public synchronized void close() {

                try {
                    socket.close();
                } catch (IOException e) {

                }
                socket = null;
                in = null;
                TCPPeer.this.currentSession = null;
                openSessions.remove(TCPPeer.this.identity());
            }

            @Override
            public synchronized boolean closed() {
                return socket == null || socket.isClosed();
            }

            @Override
            public Peer<InetSocketAddress, Bytestring> peer() {
                return TCPPeer.this;
            }
        }
    }

    private class TCPReceiver implements Runnable {
        final TCPPeer.TCPSession session;
        final InputStream in;
        final Receiver<Bytestring> receiver;

        private TCPReceiver(TCPPeer.TCPSession session, Receiver<Bytestring> receiver) {
            this.session = session;
            this.in = session.in;
            this.receiver = receiver;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    byte[] head = new byte[header.headerLength()];
                    int bytesRead = in.read(head);
                    if (bytesRead < head.length) {
                        break;
                    }

                    receiver.receive(new Bytestring(new byte[header.payloadLength(head)]));

                } catch (IOException e) {
                    break;
                }
            }

            session.close();
            return;
        }
    }

    // This contains the function that listens for new tcp connections.
    private class TCPListener implements Runnable {

        @Override
        public void run() {
            while (true) {
                // New connection found.
                Socket client = null;
                try {
                    client = server.accept();
                } catch (IOException e) {
                    return;
                }

                // Determine the identity of this connection.
                InetSocketAddress identity = new InetSocketAddress(client.getInetAddress(), client.getPort());

                TCPPeer.TCPSession session = openSessions.putOpenSession(identity, client);

                if (session == null) {
                    try {
                        client.close();
                    } catch (IOException e) {
                        continue;
                    }
                    continue;
                }

                Receiver<Bytestring> receiver = listener.newSession(session);

                if (receiver == null) {
                    continue;
                }

                executor.execute(new TCPReceiver(session, receiver));
            }
        }
    }

    Listener<InetSocketAddress, com.shuffle.p2p.Bytestring> listener;
    final int port;

    ServerSocket server;
    private boolean running = false;
    final Executor executor;

    private final Object lock = new Object();

    public TCPChannel(
            int port,
            Executor executor) {
        if (executor == null) {
            throw new NullPointerException();
        }

        this.port = port;
        this.executor = executor;
    }

    private class TCPConnection implements Connection<InetSocketAddress, Bytestring> {

        @Override
        // TODO should close all connections and stop listening.
        public void close() {
            synchronized (lock) {
                if (server != null) {
                    try {
                        server.close();
                        openSessions.closeAll();
                        openSessions = null;
                    } catch (IOException e) {

                    }
                    server = null;
                    running = false;
                }
            }
        }
    }

    @Override
    public Connection<InetSocketAddress, Bytestring> open(Listener<InetSocketAddress, com.shuffle.p2p.Bytestring> listener) {
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

            executor.execute(new TCPListener());

            return new TCPConnection();
        }
    }

    @Override
    public Peer<InetSocketAddress, com.shuffle.p2p.Bytestring> getPeer(InetSocketAddress you) {
        return peers.get(you);
    }
}
