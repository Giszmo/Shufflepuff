package com.shuffle.player;

import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.mock.MockCrypto;
import com.shuffle.mock.MockMarshaller;
import com.shuffle.monad.NaturalSummableFuture;
import com.shuffle.monad.Summable;
import com.shuffle.monad.SummableFuture;
import com.shuffle.monad.SummableFutureZero;
import com.shuffle.monad.SummableMap;
import com.shuffle.p2p.Bytestring;
import com.shuffle.p2p.Channel;
import com.shuffle.chan.Chan;
import com.shuffle.chan.ReceiveChan;
import com.shuffle.chan.SendChan;
import com.shuffle.protocol.Network;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Test class for connect.
 *
 * Created by Daniel Krawisz on 3/2/16.
 */
public class TestConnect {

    public static class ConnectRun implements Runnable {
        Channel<Integer, Bytestring> channel;
        SendChan<Network> net;
        final Connect<Integer> connect;

        final Map<Integer, VerificationKey> keys;
        final int timeout;
        final int maxRetries;

        public ConnectRun(
                Connect<Integer> connect,
                Channel<Integer, Bytestring> channel,
                Map<Integer, VerificationKey> keys,
                int timeout, int maxRetries,
                SendChan<Network> net) {
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
                Network network = connect.connect(
                    channel, keys,
                    new MockMarshaller(), timeout, maxRetries);
                if (network != null) {
                    net.send(network);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            net.close();

            connect.shutdown();
        }
    }

    public static class ConnectFuture implements Future<Summable.SummableElement<Map<Integer, Network>>> {
        final ReceiveChan<Network> netChan;
        SummableMap<Integer, Network> net = null;
        final int me;

        volatile boolean cancelled = false;

        public ConnectFuture(
                int i,
                Connect<Integer> connect,
                Channel<Integer, Bytestring> channel,
                Map<Integer, VerificationKey> keys) {
            me = i;

            Chan<Network> netChan = new Chan<Network>();
            this.netChan = netChan;

            new Thread(new ConnectRun(connect, channel, keys, 1, 3, netChan)).start();
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

        SummableMap<Integer, Network> getMap(Network net) {
            if (net == null) {
                return null;
            }

            Map<Integer, Network> map = new HashMap<>();
            map.put(me, net);
            this.net = new SummableMap<>(map);
            return this.net;
        }

        @Override
        public SummableMap<Integer, Network> get() throws InterruptedException {
            if (net != null) {
                return net;
            }

            if (netChan.closed()) {
                return null;
            }

            return getMap(netChan.receive());
        }

        @Override
        public SummableMap<Integer, Network> get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {

            if (net != null) {
                return net;
            }

            if (netChan.closed()) {
                return null;
            }

            return getMap(netChan.receive(l, timeUnit));
        }
    }

    public boolean simulation(int n, int seed) {
        if (n < 0) {
            return true;
        }

        System.out.println("Running connect test with " + n + " addresses. ");

        Crypto crypto = new MockCrypto(seed);

        // Create the set of known hosts for each player.
        Map<Integer, MockChannel<Bytestring>> knownHosts = new ConcurrentHashMap<>();
        for (int i = 1; i <= n; i++) {
            MockChannel<Bytestring> channel = new MockChannel<Bytestring>(i, knownHosts);
            knownHosts.put(i, channel);
        }

        // Create the set of keys for each player.
        Map<Integer, VerificationKey> keys = new HashMap<>();
        for (int i = 1; i <= n; i++) {
            keys.put(i, crypto.makeSigningKey().VerificationKey());
        }

        // Construct the future which represents all players trying to connect to one another.
        SummableFuture<Map<Integer, Network>> future = new SummableFutureZero<Map<Integer, Network>>();

        for (int i = 1; i <= n; i++) {
            // Make new set of keys (missing the one corresponding to this node).
            Map<Integer, VerificationKey> pKeys = new HashMap<>();
            pKeys.putAll(keys);
            pKeys.remove(i);

            future = future.plus(new NaturalSummableFuture<>(
                    new ConnectFuture(i, new Connect<Integer>(new MockCrypto(i + seed)), knownHosts.get(i), pKeys)));
        }

        // Get the result of the computation.
        Map<Integer, Network> nets = null;
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
        for(int i = 2; i < 10; i ++) {
            Assert.assertTrue(simulation(i, seed + i));
        }
    }
}
