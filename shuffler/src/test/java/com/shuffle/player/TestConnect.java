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
import com.shuffle.chan.Chan;
import com.shuffle.chan.Receive;
import com.shuffle.chan.Send;
import com.shuffle.mock.InsecureRandom;
import com.shuffle.mock.MockChannel;
import com.shuffle.mock.MockCrypto;
import com.shuffle.mock.MockSessionIdentifier;
import com.shuffle.mock.MockSigningKey;
import com.shuffle.monad.NaturalSummableFuture;
import com.shuffle.monad.Summable;
import com.shuffle.monad.SummableFuture;
import com.shuffle.monad.SummableFutureZero;
import com.shuffle.monad.SummableMap;
import com.shuffle.monad.SummableMaps;
import com.shuffle.p2p.Bytestring;
import com.shuffle.p2p.Channel;
import com.shuffle.sim.MockMarshaller;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
        private final Channel<Integer, Bytestring> channel;
        private final Send<Messages> net;
        private final Connect<Integer> connect;

        private final Map<Integer, VerificationKey> keys;
        private final int timeout;
        private final int maxRetries;

        private final SigningKey me;

        public ConnectRun(
                SigningKey me,
                Connect<Integer> connect,
                Channel<Integer, Bytestring> channel,
                Map<Integer, VerificationKey> keys,
                int timeout, int maxRetries,
                Send<Messages> net) {

            this.me = me;

            this.channel = channel;
            this.connect = connect;
            this.keys = keys;

            this.timeout = timeout;
            this.maxRetries = maxRetries;
            this.net = net;
        }

        @Override
        public void run() {
            try {
                Messages messages = connect.connect(
                    me, channel, keys,
                    new MockMarshaller(), timeout, maxRetries);
                if (messages != null) {
                    net.send(messages);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

            try {
                net.close();

                connect.shutdown();
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    public static class ConnectFuture
            implements Future<Summable.SummableElement<Map<Integer, Messages>>> {
        
        final Receive<Messages> netChan;
        SummableMap<Integer, Messages> net = null;
        final int me;

        volatile boolean cancelled = false;

        public ConnectFuture(
                int i,
                Connect<Integer> connect,
                Channel<Integer, Bytestring> channel,
                Map<Integer, VerificationKey> keys) {
            me = i;

            Chan<Messages> netChan = new BasicChan<>();
            this.netChan = netChan;

            new Thread(new ConnectRun(new MockSigningKey(i), connect, channel, keys, 1, 3, netChan)).start();
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

        SummableMap<Integer, Messages> getMap(Messages net) {
            if (net == null) {
                return null;
            }

            Map<Integer, Messages> map = new HashMap<>();
            map.put(me, net);
            this.net = new SummableMap<>(map);
            return this.net;
        }

        @Override
        public SummableMap<Integer, Messages> get() throws InterruptedException {
            if (net != null) {
                return net;
            }

            if (netChan.closed()) {
                return null;
            }

            return getMap(netChan.receive());
        }

        @Override
        public SummableMap<Integer, Messages> get(long l, TimeUnit timeUnit)
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

    private boolean simulation(int n, int seed) {
        if (n < 0) {
            return true;
        }

        System.out.println("Running connect test with " + n + " addresses. ");

        Crypto crypto = new MockCrypto(new InsecureRandom(seed));

        // Create the set of known hosts for each player.
        Map<Integer, MockChannel<Integer, Bytestring>> knownHosts = new ConcurrentHashMap<>();
        for (int i = 1; i <= n; i++) {
            MockChannel<Integer, Bytestring> channel = new MockChannel<>(i, knownHosts);
            knownHosts.put(i, channel);
        }

        // Create the set of keys for each player.
        Map<Integer, VerificationKey> keys = new HashMap<>();
        for (int i = 1; i <= n; i++) {
            keys.put(i, crypto.makeSigningKey().VerificationKey());
        }

        // Construct the future which represents all players trying to connect to one another.
        SummableFuture<Map<Integer, Messages>> future = new SummableFutureZero<>(
                new SummableMaps<Integer, Messages>()
        );

        for (int i = 1; i <= n; i++) {
            // Make new set of keys (missing the one corresponding to this node).
            Map<Integer, VerificationKey> pKeys = new HashMap<>();
            pKeys.putAll(keys);
            pKeys.remove(i);

            future = future.plus(new NaturalSummableFuture<>(
                    new ConnectFuture(i, new Connect<Integer>(
                            new MockCrypto(new InsecureRandom(i + seed)),
                            new MockSessionIdentifier("testing the connect")), knownHosts.get(i), pKeys)));
        }

        // Get the result of the computation.
        Map<Integer, Messages> nets = null;
        try {
            nets = future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return (nets != null) && nets.size() == n;
    }

    @Test
    public void testConnect() {
        int seed = 245;
        for (int i = 2; i < 10; i ++) {
            Assert.assertTrue(simulation(i, seed + i));
        }
    }
}
