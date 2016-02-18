package com.shuffle.p2p;

import com.shuffle.protocol.Message;

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
 * Receive messages over a tcp channel.
 *
 * Created by Daniel Krawisz on 1/25/16.
 */
public class TCPChannel implements Channel<InetSocketAddress, Bytestring>{

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

    public class TCPPeer implements Peer<InetSocketAddress, com.shuffle.p2p.Bytestring>{
        InetSocketAddress identity;

        Receiver<Bytestring> receiver;

        List<Session<InetSocketAddress, com.shuffle.p2p.Bytestring>> history;

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
        public InetSocketAddress identity() {
            return identity;
        }

        @Override
        public Session<InetSocketAddress, Bytestring> openSession(Receiver<Bytestring> receiver) {
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

        public class TCPSession implements Session<InetSocketAddress, com.shuffle.p2p.Bytestring> {
            Socket socket;
            InputStream in;

            public TCPSession(Socket socket) throws IOException {
                this.socket = socket;
                in = socket.getInputStream();
            }

            @Override
            public boolean send(com.shuffle.p2p.Bytestring message) {

                try {
                    socket.getOutputStream().write(message.bytes);
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

            public void check() {
                try {
                    byte[] headerBytes = new byte[header.headerLength()];
                    in.read(headerBytes);
                    byte[] payload = new byte[header.payloadLength(headerBytes)];
                    in.read(payload);
                    receiver.receive(new Bytestring(payload));
                } catch (IOException e) {
                    // TODO: do something useful here.

                }
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

            @Override
            public Peer<InetSocketAddress, Bytestring> peer() {
                return TCPPeer.this;
            }
        }
    }

    ConcurrentMap<InetSocketAddress, TCPPeer> peers = new ConcurrentHashMap<InetSocketAddress, TCPPeer>();
    InetSocketAddress me;
    int port;

    ServerSocket server;

    @Override
    public void listen(Listener<InetSocketAddress, com.shuffle.p2p.Bytestring> listener) throws IOException {
        if (listener == null) {
            throw new NullPointerException();
        }

        if (server == null) {
            server = new ServerSocket(me.getPort());
        }

        // New connection found.
        Socket client = server.accept();

        // Determine the identity of this connection.
        InetSocketAddress identity = new InetSocketAddress(client.getInetAddress(), client.getPort());

        TCPPeer peer = new TCPPeer(identity).setSesssion(client);

        peers.put(identity, peer);

        listener.newSession(peer.currentSession);
    }

    @Override
    public Peer<InetSocketAddress, com.shuffle.p2p.Bytestring> getPeer(InetSocketAddress you) {
        TCPPeer peer = peers.get(you);
        if (peer == null) {
            peer = new TCPPeer(you);
            peers.put(you, peer);
        }
        return peer;
    }
}
