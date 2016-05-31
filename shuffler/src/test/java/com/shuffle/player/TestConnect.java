/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.player;

import com.shuffle.bitcoin.Crypto;
import com.shuffle.chan.BasicChan;
import com.shuffle.chan.Chan;
import com.shuffle.chan.Inbox;
import com.shuffle.chan.Receive;
import com.shuffle.chan.Send;
import com.shuffle.mock.InsecureRandom;
import com.shuffle.mock.MockNetwork;
import com.shuffle.mock.MockCrypto;
import com.shuffle.monad.NaturalSummableFuture;
import com.shuffle.monad.Summable;
import com.shuffle.monad.SummableFuture;
import com.shuffle.monad.SummableFutureZero;
import com.shuffle.monad.SummableMap;
import com.shuffle.monad.SummableMaps;
import com.shuffle.p2p.Channel;
import com.shuffle.p2p.Collector;
import com.shuffle.p2p.Connect;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

/**
 * Test class for connect.
 *
 * Created by Daniel Krawisz on 3/2/16.
 */
public class TestConnect {

    public static class ConnectRun implements Runnable {
        private final Send<Collector<Integer, String>> net;
        private final Connect<Integer, String> conn;

        private final SortedSet<Integer> addresses;
        private final int maxRetries;

        public ConnectRun(
                Connect<Integer, String> conn,
                SortedSet<Integer> addresses,
                int maxRetries,
                Send<Collector<Integer, String>> net) {

            if (conn == null || net == null)
                throw new NullPointerException();

            this.addresses = addresses;
            this.maxRetries = maxRetries;
            this.net = net;
            this.conn = conn;
        }

        @Override
        public void run() {
            try {

                Collector<Integer, String> m = conn.connect(addresses, maxRetries);

                if (m != null) {
                    net.send(m);
                }
            } catch (IOException | InterruptedException | NullPointerException e) {
                e.printStackTrace();
            }

            try {
                net.close();
            } catch (InterruptedException e) {}
        }
    }

    public static class ConnectFuture
            implements Future<Summable.SummableElement<Map<Integer, Collector<Integer, String>>>> {
        
        final Receive<Collector<Integer, String>> netChan;
        SummableMap<Integer, Collector<Integer, String>> net = null;

        volatile boolean cancelled = false;
        
        int me;

        public ConnectFuture(
                int i,
                Connect<Integer, String> conn,
                SortedSet<Integer> addresses) throws InterruptedException {

            if (conn == null || addresses == null)
                throw new NullPointerException();

            me = i;

            Chan<Collector<Integer, String>> netChan = new BasicChan<>();
            this.netChan = netChan;

            new Thread(new ConnectRun(conn, addresses, 3, netChan)).start();
        }

        @Override
        public boolean cancel(boolean b) {
            // TODO
            return false;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return net != null || netChan.closed();
        }

        SummableMap<Integer, Collector<Integer, String>> getMap(Collector<Integer, String> net) {
            if (net == null) {
                return null;
            }

            Map<Integer, Collector<Integer, String>> map = new HashMap<>();
            map.put(me, net);
            this.net = new SummableMap<>(map);
            return this.net;
        }

        @Override
        public SummableMap<Integer, Collector<Integer, String>> get() throws InterruptedException {
            if (net != null) {
                return net;
            }

            if (netChan.closed()) {
                return null;
            }

            return getMap(netChan.receive());
        }

        @Override
        public SummableMap<Integer, Collector<Integer, String>> get(long l, TimeUnit timeUnit)
                throws InterruptedException, ExecutionException, TimeoutException {

            if (net != null) {
                return net;
            }

            if (netChan.closed()) {
                return null;
            }

            return getMap(netChan.receive(l, timeUnit));
        }
    }

    private Map<Integer, Collector<Integer, String>> simulation(int n, int seed) throws InterruptedException {
        if (n <= 0) {
            return new HashMap<>();
        }

        System.out.println("Running connect test with " + n + " addresses. ");

        Crypto crypto = new MockCrypto(new InsecureRandom(seed));

        MockNetwork<Integer, String> mock = new MockNetwork<>();
        SortedSet<Integer> addresses = new TreeSet<>();

        // Create the set of known hosts for each player.
        for (int i = 1; i <= n; i++) {
            addresses.add(i);
        }

        // Construct the future which represents all players trying to connect to one another.
        SummableFuture<Map<Integer, Collector<Integer, String>>> future = new SummableFutureZero<>(
                new SummableMaps<Integer, Collector<Integer, String>>()
        );

        // Create the set of known hosts for each player.
        Map<Integer, Connect<Integer, String>> connections = new HashMap<>();
        for (Integer i : addresses) {
            Channel<Integer, String> channel = mock.node(i);
            Assert.assertNotNull(channel);
            Connect<Integer, String> conn = new Connect<>(channel, crypto, 10);
            connections.put(i, conn);
        }

        // Start the connection (this must be done after all Channel objects have been created
        // because everyone must be connected to the internet at the time they attempt to start
        // connecting to one another.
        for (Map.Entry<Integer, Connect<Integer, String>> e : connections.entrySet()) {
            future = future.plus(new NaturalSummableFuture<>(
                    new ConnectFuture(e.getKey(), e.getValue(), addresses)));
        }

        // Get the result of the computation.
        Map<Integer, Collector<Integer, String>> nets = null;
        try {
            nets = future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return nets;
    }

    @Test
    public void testConnect() throws IOException, InterruptedException {
        int seed = 245;
        int msgNo = 100;
        for (int i = 2; i <= 13; i ++) {
            System.out.println("Trial " + i + ": ");
            Map<Integer, Collector<Integer, String>> nets = simulation(i, seed + i);
            Assert.assertTrue(nets != null);
            System.out.println("Trial " + i + ": " + nets);
            Assert.assertTrue(nets.size() == i);

            // Check that messages can be sent in all directions.
            for (Map.Entry<Integer, Collector<Integer, String>> e : nets.entrySet()) {
                Integer from = e.getKey();
                Collector<Integer, String> sender = e.getValue();

                for (Map.Entry<Integer, Collector<Integer, String>> a : nets.entrySet()) {
                    Integer to = a.getKey();
                    if (from.equals(to)) continue;
                    System.out.println("  Sending messages between " + from + " and " + to);

                    Collector<Integer, String> recipient = a.getValue();

                    String j = "Oooo! " + msgNo;
                    sender.connected.get(to).send(j);
                    Inbox.Envelope<Integer, String> q = recipient.inbox.receive();

                    Assert.assertNotNull(q);
                    Assert.assertTrue(q.from.equals(from));
                    Assert.assertTrue(q.payload.equals(j));
                    msgNo ++;
                }
            }
        }
    }
}
