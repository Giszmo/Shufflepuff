/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.player;

import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.BasicChan;
import com.shuffle.chan.Chan;
import com.shuffle.p2p.Bytestring;
import com.shuffle.p2p.Channel;
import com.shuffle.p2p.Connection;
import com.shuffle.p2p.Peer;
import com.shuffle.p2p.Receiver;
import com.shuffle.p2p.Session;
import com.shuffle.protocol.FormatException;
import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.SignedPacket;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * A class for setting up Network objects. It manages setting up all all the necessary
 * connections between peers.
 *
 * Created by Daniel Krawisz on 2/16/16.
 */
public class Connect<Identity> {
    private final Crypto crypto;

    public Connect(Crypto crypto) {
        this.crypto = crypto;
    }

    // Provides functions for a thread to call when it receives a new connection.
    private class Listener implements com.shuffle.p2p.Listener<Identity, Bytestring> {
        final Receiver<Bytestring> receiver;
        final Map<VerificationKey, Session<Identity, Bytestring>> players = new HashMap<>();
        final Peers peers;
        final Map<Identity, VerificationKey> keys;

        private Listener(Peers peers,
                         Map<Identity, VerificationKey> keys,
                         Receiver<Bytestring> receiver) {
            this.peers = peers;
            this.receiver = receiver;
            this.keys = keys;
        }

        @Override
        public Receiver<Bytestring> newSession(Session<Identity, Bytestring> session) {
            VerificationKey key = keys.get(session.peer().identity());

            if (peers.remove(session.peer())) {
                players.put(key, session);
            }

            return receiver;
        }
    }

    // Keep track of the number of connection attempt retries for each address.
    private class Retries {
        final Map<Peer<Identity, Bytestring>, Integer> retries = new HashMap<>();

        public int retries(Peer<Identity, Bytestring> peer) {
            Integer r = retries.get(peer);
            if (r == null) {
                retries.put(peer, 0);
                return 0;
            }

            return r;
        }

        public int increment(Peer<Identity, Bytestring> peer) {
            Integer r = retries.get(peer);

            if (r == null) {
                r = 0;
            }
            r++;

            retries.put(peer, r);
            return r;
        }
    }

    // The list of peers will be altered by two threads; the one for initiating connections
    // and the one for receiving connections. We set it in its own class to allow for some
    // synchronized functions.
    private class Peers {
        private final Queue<Peer<Identity, Bytestring>> peers = new LinkedList<>();

        private Peers() { }

        public void add(Peer<Identity, Bytestring> peer) {
            if (peer == null) {
                throw new NullPointerException();
            }
            peers.add(peer);
        }

        public Peer<Identity, Bytestring> peek() {
            return peers.peek();
        }

        synchronized boolean rotate() {
            Peer<Identity, Bytestring> peer = peers.poll();
            if (peer == null) {
                return false;
            }

            peers.add(peer);
            return true;
        }

        synchronized boolean remove(Peer<Identity, Bytestring> peer) {
            return peers.remove(peer);
        }

        @Override
        public String toString() {
            return peers.toString();
        }
    }

    private Connection<Identity, Bytestring> connection;

    // Connect to all peers; remote peers can be initiating connections to us as well.
    public com.shuffle.protocol.Network connect(
            Channel<Identity, Bytestring> channel,
            Map<Identity, VerificationKey> keys,
            Marshaller<Bytestring> marshall,
            int timeout,
            int maxRetries) throws IOException, InterruptedException {

        if (connection != null) {
            return null;
        }

        Peers peers = new Peers();

        Map<VerificationKey, Session<Identity, Bytestring>> players = new HashMap<>();

        Network<Identity> network = new Network<>(players, marshall, timeout);

        Listener listener = new Listener(peers, keys, network);

        connection = channel.open(listener);
        // TODO need to be able to disconnect the network.
        if (connection == null) {
            throw new IOException();
        }

        // Randomly arrange the list of peers.
        // First, put all peers in an array.
        int size = keys.size();
        Peer<Identity, Bytestring>[] p = new Peer[size];
        int i = 0;
        for (Identity identity : keys.keySet()) {
            Peer<Identity, Bytestring> peer = channel.getPeer(identity);
            if (peer == null) {
                throw new NullPointerException();
            }
            p[i] = peer;
            i++;
        }

        // Then randomly select them one at a time and put them in peers.
        i = 0;
        for (int rmax = size - 1; rmax >= 0; rmax--) {
            int rand = crypto.getRandom(rmax);

            peers.add(p[rand]);

            p[rand] = p[rmax];
        }

        final Retries retries = new Retries();

        int l = 0;
        while (true) {
            Peer<Identity, Bytestring> peer = peers.peek();
            if (peer == null) {
                break;
            }

            if (peer.open()) {
                peers.remove(peer);
                continue;
            }

            Session<Identity, Bytestring> session = peer.openSession(network);

            if (session != null) {
                peers.remove(peer);
                listener.newSession(session);
                continue;
            }

            int r = retries.increment(peer);

            if (r > maxRetries) {
                // Maximum number of retries has prevented us from making all connections.
                // TODO In some instances, it should be possible to run coin shuffle with fewer
                // players, so we should still return the network object.
                return null;
            }

            // We were not able to connect to this peer this time,
            // so we move on to the next one for now.
            if (!peers.rotate()) {
                break;
            }
        }

        return network;
    }

    public void shutdown() {
        if (connection == null) {
            return;
        }

        connection.close();
        connection = null;
    }

    /**
     * An implementation of Network which connects the interface defined in com.shuffle.protocol
     * to the channel through which the communication takes place. It manages the opening of
     * channels and the marshalling of messages.
     *
     * Created by Daniel Krawisz on 2/13/16.
     */
    private static class Network<Identity>
            implements com.shuffle.protocol.Network, Receiver<Bytestring> {

        final Map<VerificationKey, Session<Identity, Bytestring>> players;
        final Marshaller<Bytestring> marshall;
        final Chan<Bytestring> received = new BasicChan<>();
        final int timeout;

        Network(Map<VerificationKey, Session<Identity, Bytestring>> players,
                Marshaller<Bytestring> marshall, int timeout) {
            this.players = players;
            this.marshall = marshall;
            this.timeout = timeout;

        }

        // Network.
        //
        // This is the part that connects to the protocol. It allows the protocol to send and
        // receive when it needs to without thinking about what's going on underneith.

        @Override
        public void sendTo(VerificationKey to, SignedPacket packet)
                throws InvalidImplementationError, InterruptedException {

            Session<Identity, Bytestring> session = players.get(to);

            if (session == null) {
                throw new InvalidImplementationError();
            }

            session.send(marshall.marshall(packet));
        }

        @Override
        public SignedPacket receive()
                throws InvalidImplementationError,
                InterruptedException, FormatException {

            Bytestring str = received.receive(timeout, TimeUnit.SECONDS);
            return marshall.unmarshall(str);
        }

        // Receiver<Bytestring>.
        //
        // We collect all messages from everybody in a central queue.

        @Override
        public void receive(Bytestring bytestring) throws InterruptedException {
            received.send(bytestring);
        }
    }
}
