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
import java.util.concurrent.ConcurrentMap;

/**
 * Created by cosmos on 1/25/16.
 */
public class TCPChannel implements Channel<InetSocketAddress, Bytestring, Void>{

    private class Bytestring implements com.shuffle.p2p.Bytestring {
        byte[] bytes;

        public Bytestring(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }
    }

    // A message sent over TCP requires a header to tell how long it is.
    public interface Header {
        int headerLength();

        int payloadLength(byte[] header);

        com.shuffle.p2p.Bytestring makeHeader(int payloadLength);
    }

    // A particular header that is used for this particular channel.
    Header header;

    // The sessions which are currently open.
    Map<InetSocketAddress, TCPPeer.TCPSession> openSessions = new HashMap<>();

    public class TCPPeer implements Peer<InetSocketAddress, com.shuffle.p2p.Bytestring, Void>{
        InetSocketAddress identity;

        List<Session<com.shuffle.p2p.Bytestring, Void>> history;

        TCPSession currentSession;

        public TCPPeer(InetSocketAddress identity) {
            this.identity = identity;
        }

        public TCPPeer(InetSocketAddress identity, TCPSession session) {
            this.identity = identity;
            this.currentSession = session;
        }

        private TCPPeer setSesssion(Socket socket) throws IOException {
            currentSession = new TCPSession(socket);
            return this;
        }

        @Override
        public InetSocketAddress getIdentity() {
            return identity;
        }

        @Override
        public Session<com.shuffle.p2p.Bytestring, Void> openSession() {
            if (currentSession != null) {
                return null;
            }

            try {
                Socket socket = new Socket(identity.getAddress(), identity.getPort());
                currentSession = new TCPSession(socket);
            } catch (IOException e) {
                // TODO handle this appropriately. Number of timeouts etc.
            }
            return currentSession;
        }

        public class TCPSession implements Session<com.shuffle.p2p.Bytestring, Void> {
            Socket socket;
            InputStream in;

            public TCPSession(Socket socket) throws IOException {
                this.socket = socket;
                in = socket.getInputStream();
            }

            @Override
            public boolean send(com.shuffle.p2p.Bytestring message) {

                try {
                    socket.getOutputStream().write(message.getBytes());
                } catch (IOException e) {
                    return false;
                }

                return true;
            }

            @Override
            public boolean ready() {
                try {
                    return in.available() > 0;
                } catch (IOException e) {
                    return false;
                }
            }

            @Override
            public com.shuffle.p2p.Bytestring receive() {
                try {
                    byte[] headerBytes = new byte[header.headerLength()];
                    in.read(headerBytes);
                    byte[] payload = new byte[header.payloadLength(headerBytes)];
                    in.read(payload);
                    return new com.shuffle.p2p.TCPChannel.Bytestring(payload);
                } catch (IOException e) {
                    // TODO: do something useful here.
                    return null;
                }
            }

            @Override
            public Void getToken() {
                return null;
            }

            @Override
            public void close() {
                try {
                    socket.close();
                } catch (IOException e) {
                    // TODO do something useful.
                }
                socket = null;
                in = null;
                TCPPeer.this.currentSession = null;
                openSessions.remove(this);
            }

            @Override
            public boolean isOpen() {
                return socket.isConnected();
            }
        }
    }

    ConcurrentMap<InetSocketAddress, TCPPeer> peers = new ConcurrentHashMap<InetSocketAddress, TCPPeer>();
    InetSocketAddress me;
    int port;

    TCPListener listener;

    private class TCPListener implements Runnable {
        Listener<InetSocketAddress, com.shuffle.p2p.Bytestring, Void> listener;
        ServerSocket server;

        private TCPListener(int port, Listener<InetSocketAddress, com.shuffle.p2p.Bytestring, Void> listener) throws IOException {
            this.listener = listener;
            server = new ServerSocket(me.getPort());
        }

        @Override
        public void run() {
            try {
                while (true) {
                    // New connection found.
                    Socket client = server.accept();

                    // Determine the identity of this connection.
                    InetSocketAddress identity = new InetSocketAddress(client.getInetAddress(), client.getPort());

                    TCPPeer peer = new TCPPeer(identity).setSesssion(client);

                    peers.put(identity, peer);

                    listener.newPeer(getPeer(identity));
                }
            } catch (IOException e) {
                // TODO
            }
        }

        public void close() {
            try {
                server.close();
            } catch (IOException e) {
                // TODO
            }
        }
    }

    @Override
    public boolean listen(Listener<InetSocketAddress, com.shuffle.p2p.Bytestring, Void> listener) {
        if (this.listener != null) return false;

        try {
            this.listener = new TCPListener(port, listener);
        } catch (IOException e) {
            return false;
        }

        Thread thread = new Thread(this.listener);
        thread.start();

        return true;
    }

    @Override
    public Peer<InetSocketAddress, com.shuffle.p2p.Bytestring, Void> getPeer(InetSocketAddress you) {
        TCPPeer peer = peers.get(you);
        if (peer == null) {
            peer = new TCPPeer(you);
            peers.put(you, peer);
        }
        return peer;
    }
}
