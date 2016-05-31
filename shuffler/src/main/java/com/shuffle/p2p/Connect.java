/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;

import com.shuffle.bitcoin.Crypto;
import com.shuffle.chan.BasicInbox;
import com.shuffle.chan.Send;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A class for setting up Network objects. It manages setting up all all the necessary
 * connections between peers.
 *
 * Created by Daniel Krawisz on 2/16/16.
 */
public class Connect<Address, P extends Serializable> implements Connection<Address> {

    // The list of peers will be altered by two threads; the one for initiating connections
    // and the one for receiving connections. We set it in its own class to allow for some
    // synchronized functions.
    private class Peers {

        // The list of peers which we have not connected with yet.
        private final Queue<Address> unconnected = new LinkedList<>();
        private final Set<Address> remaining = new TreeSet<>();

        private final Collector<Address, P> collector;

        private Peers(Collector<Address, P> collector) {
            this.collector = collector;
        }

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

        private boolean connect(Session<Address, P> session) throws InterruptedException {
            Address addr = session.peer().identity();

            return remaining.remove(addr) && collector.put(session);

        }

        public synchronized boolean openSession(
                Address address,
                Peer<Address, P> peer) throws InterruptedException {

            Send<P> processor = collector.inbox.receivesFrom(address);
            if (processor != null) {
                Session<Address, P> session =
                        peer.openSession(processor);

                if (session != null) {
                    remove();
                    connect(session);
                    return true;
                } else {
                    processor.close();
                    return false;
                }
            }
            return false;
        }

        boolean connected(Address address) {
            return collector.connected.containsKey(address);
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

    private final Channel<Address, P> channel;
    private final Connection<Address> connection;
    private final Collector<Address, P> collector;
    private final Crypto crypto;

    private boolean finished = false;

    public Connect(Channel<Address, P> channel, Crypto crypto, int bloop) throws InterruptedException {
        if (channel == null || crypto == null) throw new NullPointerException();

        collector = new Collector<>(new BasicInbox<Address, P>(bloop));

        connection = channel.open(collector);
        if (connection == null) throw new IllegalArgumentException();

        this.channel = channel;
        this.crypto = crypto;
    }

    // Connect to all peers; remote peers can be initiating connections to us as well.
    public Collector<Address, P> connect(
            SortedSet<Address> addrs,
            int maxRetries) throws IOException, InterruptedException {

        if (addrs == null) throw new NullPointerException();

        if (finished) return null;

        // TODO make there be a parameter for max messages rather than just doing 100.
        Peers peers = new Peers(collector);

        Address me = channel.identity();

        // Randomly arrange the list of peers.
        // First, put all peers in an array.
        ArrayList<Address> addresses = new ArrayList<>();
        addresses.addAll(addrs);

        // Then randomly select them one at a time and put them in peers.
        for (int rmax = addrs.size() - 1; rmax >= 0; rmax--) {
            int rand = crypto.getRandom(rmax);
            Address addr = addresses.get(rand);

            // Put the address at the end into the spot we just took. This way,
            // we are always selecting randomly from a set of unselected peers.
            addresses.set(rand, addresses.get(rmax));

            // Don't try to connect to myself.
            if (addr.equals(me)) continue;

            peers.queue(addr);
        }

        final Retries retries = new Retries();

        int l = 0;
        while (true) {
            Address address = peers.peek();
            if (address == null) {
                break;
            }

            if (peers.connected(address)) {
                peers.remove();
                continue;
            }

            Peer<Address, P> peer = channel.getPeer(address);

            if (peer == null) {
                // TODO clean up properly and fail more gracefully.
                throw new NullPointerException();
            }

            if (peers.openSession(address, peer)) {
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

        finished = true;
        return collector;
    }

    @Override
    public Address identity() {
        return connection.identity();
    }

    @Override
    public void close() throws InterruptedException {
        connection.close();
    }

    @Override
    public boolean closed() throws InterruptedException {
        return connection.closed();
    }
}
