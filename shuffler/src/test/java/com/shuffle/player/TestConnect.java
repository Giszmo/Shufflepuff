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
import com.shuffle.protocol.Network;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
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
        final LinkedBlockingQueue<Network> net;
        final Connect<Integer> connect;

        final Map<Integer, VerificationKey> keys;
        final int timeout;
        final int maxRetries;

        public ConnectRun(
                Connect<Integer> connect,
                Channel<Integer, Bytestring> channel,
                Map<Integer, VerificationKey> keys,
                int timeout, int maxRetries,
                LinkedBlockingQueue net) {
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
                net.put(connect.connect(
                        channel, keys,
                        new MockMarshaller(), timeout, maxRetries));
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }

            connect.shutdown();
        }
    }

    public static class ConnectFuture implements Future<Summable.SummableElement<Map<Integer, Network>>> {
        final LinkedBlockingQueue<Network> netChan = new LinkedBlockingQueue<>();
        Network net = null;
        final int me;

        volatile boolean cancelled = false;

        public ConnectFuture(
                int i,
                Connect<Integer> connect,
                Channel<Integer, Bytestring> channel,
                Map<Integer, VerificationKey> keys) {
            me = i;

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
            return net != null || netChan.size() > 0;
        }

        SummableMap<Integer, Network> getMap(Network net) {
            if (net == null) {
                return null;
            }

            Map<Integer, Network> map = new HashMap<>();
            map.put(me, net);
            return new SummableMap<>(map);
        }

        @Override
        public SummableMap<Integer, Network> get() throws InterruptedException, ExecutionException {
            return getMap(netChan.poll());

        }

        @Override
        public SummableMap<Integer, Network> get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
            return getMap(netChan.poll(l, timeUnit));
        }
    }

    public boolean simulation(int n, int seed) {
        if (n < 0) {
            return true;
        }

        Crypto crypto = new MockCrypto(seed);

        Map<Integer, VerificationKey> keys = new HashMap<>();
        for (int i = 1; i <= n; i++) {
            keys.put(i, crypto.makeSigningKey().VerificationKey());
        }

        Map<Integer, MockChannel> knownHosts = new HashMap<>();
        SummableFuture<Map<Integer, Network>> future = new SummableFutureZero<Map<Integer, Network>>();
        for (int i = 1; i <= n; i++) {
            MockChannel channel = new MockChannel(i, knownHosts);
            knownHosts.put(i, new MockChannel(i, knownHosts));

            // Make new set of keys (missing the one corresponding to this node).
            Map<Integer, VerificationKey> pKeys = new HashMap<>();
            pKeys.putAll(keys);
            pKeys.remove(i);

            future = future.plus(new NaturalSummableFuture<>(
                    new ConnectFuture(i, new Connect<Integer>(new MockCrypto(i + seed)), channel, pKeys)));
        }

        Map<Integer, Network> nets = null;
        try {
            nets = future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }



        return true;
    }

    public void testConnect() {
    }
}
