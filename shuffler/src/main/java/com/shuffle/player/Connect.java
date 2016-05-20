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
import com.shuffle.chan.Send;
import com.shuffle.chan.SigningSend;
import com.shuffle.chan.VerifyingSend;
import com.shuffle.p2p.Bytestring;
import com.shuffle.p2p.Channel;
import com.shuffle.p2p.Connection;
import com.shuffle.p2p.Listener;
import com.shuffle.chan.Inbox;
import com.shuffle.p2p.Peer;
import com.shuffle.p2p.Session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

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
        // The set of connected peers.
        private final Map<VerificationKey, Send<Packet>> connected = new HashMap<>();

        // The list of peers which we have not connected with yet.
        private final Queue<Address> unconnected = new LinkedList<>();
        private final Set<Address> remaining = new TreeSet<>();

        private Peers() {   }

        public void queue(Address address) {
            if (address == null) {
                throw new NullPointerException();
            }

            if (remaining.contains(address)) return;

            unconnected.add(address);
            remaining.add(address);
        }

        public Address peek() {
            return unconnected.peek();
        }

        boolean rotate() {
            Address address = unconnected.poll();
            if (address == null) {
                return false;
            }

            unconnected.add(address);
            return true;
        }

        synchronized boolean connect(Address addr, VerificationKey k, Send<Packet> p) {

            if (!remaining.remove(addr)) {
                return false;
            }

            connected.put(k, p);
            return true;
        }

        boolean connected(Address address) {
            return !remaining.contains(address);
        }

        // Removes the first element from the list.
        void remove() {
            unconnected.remove();
        }


        @Override
        public String toString() {
            return unconnected.toString();
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

    // Provides functions to be called when a new connection is received.
    private class SigningListener implements Listener<Address, Bytestring> {
        private final SigningKey me;
        private final Map<Address, VerificationKey> keys;
        private final Peers peers;
        private final SigningSend.Marshaller<Packet> marshaller;
        private final Inbox<VerificationKey, VerifyingSend.Signed<Packet>> inbox;

        private SigningListener(
                SigningKey me,
                Peers peers,
                Map<Address, VerificationKey> keys,
                SigningSend.Marshaller<Packet> marshaller,
                Inbox<VerificationKey, VerifyingSend.Signed<Packet>> inbox) {

            if (me == null || peers == null || keys == null
                    || marshaller == null || inbox == null) throw new NullPointerException();

            this.me = me;
            this.peers = peers;
            this.keys = keys;
            this.marshaller = marshaller;
            this.inbox = inbox;
        }

        @Override
        public Send<Bytestring> newSession(Session<Address, Bytestring> session)
                throws InterruptedException {

            VerificationKey key = keys.get(session.peer().identity());

            if (key == null) {
                session.close();
                return null;
            }

            if (!peers.connect(session.peer().identity(), key,
                    new SigningSend<>(session, marshaller, me))) {

                session.close();
                return null;
            }

            return new VerifyingSend<>(inbox.receivesFrom(key), marshaller, key);
        }
    }

    private Connection<Address> connection;

    public Connect(Crypto crypto, SessionIdentifier session) {
        this.crypto = crypto;
        this.session = session;
    }

    // Connect to all peers; remote peers can be initiating connections to us as well.
    public Messages connect(
            SigningKey me,
            Channel<Address, Bytestring> channel,
            Map<Address, VerificationKey> keys,
            SigningSend.Marshaller<Packet> marshall,
            int maxRetries) throws IOException, InterruptedException {

        if (connection != null) {
            return null;
        }

        Peers peers = new Peers();

        Map<VerificationKey, Send<Packet>> players = new HashMap<>();

        Inbox<VerificationKey, VerifyingSend.Signed<Packet>> inbox = new Inbox<>(100);

        Listener<Address, Bytestring> listener = new SigningListener(me, peers, keys, marshall, inbox);

        connection = channel.open(listener);
        // TODO need to be able to disconnect the network.
        if (connection == null) {
            throw new IOException();
        }

        // Randomly arrange the list of peers.
        // First, put all peers in an array.
        ArrayList<Address> addresses = new ArrayList<>();
        for (Address address : keys.keySet()) {
            addresses.add(address);
        }

        // Then randomly select them one at a time and put them in peers.
        for (int rmax = keys.size() - 1; rmax >= 0; rmax--) {
            int rand = crypto.getRandom(rmax);

            peers.queue(addresses.get(rand));

            // Put the address at the end into the spot we just took.
            addresses.set(rand, addresses.get(rmax));
        }

        final Retries retries = new Retries();

        int l = 0;
        while (true) {
            Address address = peers.peek();
            if (address == null) {
                break;
            }

            Peer<Address, Bytestring> peer = channel.getPeer(address);

            if (peer == null) {

                // TODO clean up properly and fail more gracefully.
                throw new NullPointerException();
            }

            if (!peers.connected(address)) {
                peers.remove();
                continue;
            }

            VerificationKey key = keys.get(address);
            Send<VerifyingSend.Signed<Packet>> processor = inbox.receivesFrom(key);
            if (processor != null) {
                Session<Address, Bytestring> session =
                        peer.openSession(new VerifyingSend<Packet>(processor, marshall, key));

                if (session != null) {
                    peers.remove();
                    listener.newSession(session);
                    continue;
                }
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

        return new Messages(session, me.VerificationKey(), players, inbox);
    }

    public void shutdown() throws InterruptedException {
        if (connection == null) {
            return;
        }

        connection.close();
        connection = null;
    }
}
