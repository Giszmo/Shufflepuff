/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.player;

import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.BasicChan;
import com.shuffle.chan.Send;
import com.shuffle.p2p.Bytestring;
import com.shuffle.p2p.Channel;
import com.shuffle.p2p.Connection;
import com.shuffle.p2p.Peer;
import com.shuffle.p2p.Session;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * A class for setting up Network objects. It manages setting up all all the necessary
 * connections between peers.
 *
 * Created by Daniel Krawisz on 2/16/16.
 */
public class Connect<Address> {

    private final Crypto crypto;
    private final SessionIdentifier session;

    // The list of peers will be altered by two threads; the one for initiating connections
    // and the one for receiving connections. We set it in its own class to allow for some
    // synchronized functions.
    private class Peers {
        private final Queue<Peer<Address, Bytestring>> peers = new LinkedList<>();

        private Peers() { }

        public void add(Peer<Address, Bytestring> peer) {
            if (peer == null) {
                throw new NullPointerException();
            }
            peers.add(peer);
        }

        public Peer<Address, Bytestring> peek() {
            return peers.peek();
        }

        synchronized boolean rotate() {
            Peer<Address, Bytestring> peer = peers.poll();
            if (peer == null) {
                return false;
            }

            peers.add(peer);
            return true;
        }

        synchronized boolean remove(Peer<Address, Bytestring> peer) {
            return peers.remove(peer);
        }

        @Override
        public String toString() {
            return peers.toString();
        }
    }

    // Keep track of the number of connection attempt retries for each address.
    private class Retries {
        final Map<Address, Integer> retries = new HashMap<>();

        public int retries(Address address) {
            Integer r = retries.get(address);
            if (r == null) {
                retries.put(address, 0);
                return 0;
            }

            return r;
        }

        public int increment(Address address) {
            Integer r = retries.get(address);

            if (r == null) {
                r = 0;
            }
            r++;

            retries.put(address, r);
            return r;
        }
    }

    // Provides functions for a thread to call when it receives a new connection.
    private class Listener implements com.shuffle.p2p.Listener<Address, Bytestring> {
        final SignedReceiver receiver;
        final Map<Address, VerificationKey> keys;
        final Map<VerificationKey, Send<Packet>> players;
        final Peers peers;
        final Marshaller marshaller;

        private Listener(Map<VerificationKey, Send<Packet>> players,
                         Peers peers,
                         Map<Address, VerificationKey> keys,
                         SignedReceiver receiver,
                         Marshaller marshaller) {

            this.players = players;
            this.peers = peers;
            this.keys = keys;
            this.receiver = receiver;
            this.marshaller = marshaller;
        }

        @Override
        public Send<Bytestring> newSession(Session<Address, Bytestring> session) throws InterruptedException {
            VerificationKey key = keys.get(session.peer().identity());

            if (key == null) {
                session.close();
            }

            if (peers.remove(session.peer())) {
                players.put(key, new SignedSender<Address>(session, marshaller));
            }

            return receiver;
        }
    }

    private Connection<Address, Bytestring> connection;

    public Connect(Crypto crypto, SessionIdentifier session) {
        this.crypto = crypto;
        this.session = session;
    }

    // Connect to all peers; remote peers can be initiating connections to us as well.
    public Messages connect(
            SigningKey me,
            Channel<Address, Bytestring> channel,
            Map<Address, VerificationKey> keys,
            Marshaller marshall,
            int timeout,
            int maxRetries) throws IOException, InterruptedException {

        if (connection != null) {
            return null;
        }

        Peers peers = new Peers();

        Map<VerificationKey, Send<Packet>> players = new HashMap<>();

        SignedReceiver receiver = new SignedReceiver(marshall, new BasicChan<Bytestring>(2 * (1 + players.size())));

        Listener listener = new Listener(players, peers, keys, receiver, marshall);

        connection = channel.open(listener);
        // TODO need to be able to disconnect the network.
        if (connection == null) {
            throw new IOException();
        }

        // Randomly arrange the list of peers.
        // First, put all peers in an array.
        int size = keys.size();
        Peer<Address, Bytestring>[] p = new Peer[size];
        int i = 0;
        for (Address address : keys.keySet()) {
            Peer<Address, Bytestring> peer = channel.getPeer(address);
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
            Peer<Address, Bytestring> peer = peers.peek();
            if (peer == null) {
                break;
            }

            if (peer.open()) {
                peers.remove(peer);
                continue;
            }

            Session<Address, Bytestring> session = peer.openSession(receiver);

            if (session != null) {
                peers.remove(peer);
                listener.newSession(session);
                continue;
            }

            int r = retries.increment(peer.identity());

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

        return new Messages(session, me.VerificationKey(), players, receiver);
    }

    public void shutdown() throws InterruptedException {
        if (connection == null) {
            return;
        }

        connection.close();
        connection = null;
    }
}
