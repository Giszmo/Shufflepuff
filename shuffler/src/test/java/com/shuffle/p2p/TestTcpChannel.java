package com.shuffle.p2p;

import com.shuffle.chan.BasicChan;
import com.shuffle.chan.Chan;
import com.shuffle.chan.Receive;
import com.shuffle.chan.Send;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Tests for the tcp connection
 *
 * Created by Daniel Krawisz on 4/14/16.
 */
public class TestTcpChannel {

    // The three channels we will use to test with.
    // This is a vector.
    Channel<Integer, Integer>[] channels;

    // The three connections we would like to open.
    // This is also a vector.
    Connection<Integer, Integer>[] conn;

    // The peers that represent the programs' view of one anothers
    // This is an assymmetric 2-tensor.
    Peer<Integer, Integer>[][] peer;

    // The session from A to B and the session from B to A.
    // Also an assymmetric 2-tensor.
    Session<Integer, Integer>[][] session;

    // Channels for receiving messages.
    // another assymmetrit 2-tensor.
    Receive<Integer> rec[][];

    // The connection listeners. A vector.
    TestListener listen[];

    private static class TcpTestHeader implements TcpChannel.Header {

        @Override
        public int headerLength() {
            return 0;
        }

        @Override
        public int payloadLength(byte[] header) throws IOException {
            return 4;
        }

        @Override
        public Bytestring makeHeader(int payloadLength) throws IOException {
            return new Bytestring(new byte[]{});
        }
    }

    private static class TcpTestChannel implements Channel<Integer, Integer> {
        private final TcpChannel tcp;
        private final Integer me;
        private final int[] ports;

        TcpTestChannel(Integer me, int[] ports, Executor exec)
                throws UnknownHostException {

            this.me = me;
            this.ports = ports;
            tcp = new TcpChannel(
                    new TcpTestHeader(),
                    new InetSocketAddress(InetAddress.getLocalHost(), ports[me]),
                    exec);

        }

        private class TcpTestPeer implements Peer<Integer, Integer> {
            private final Peer<InetSocketAddress, Bytestring> peer;

            private TcpTestPeer(Peer<InetSocketAddress, Bytestring> peer) {
                this.peer = peer;
            }

            @Override
            public Integer identity() {
                return peer.identity().getPort();
            }

            @Override
            public Session<Integer, Integer> openSession(Send<Integer> send)
                    throws InterruptedException {

                Session<InetSocketAddress, Bytestring> p = peer.openSession(new TcpTestReceiver(send));

                if (p == null) {
                    return null;
                }

                Session<Integer, Integer> session =
                        new TcpTestSession(p);

                // Tell them who we are.
                session.send(me);

                return session;
            }

            @Override
            public boolean open() throws InterruptedException {
                return peer.open();
            }

            @Override
            public void close() throws InterruptedException {
                peer.close();
            }
        }

        private class TcpTestSession implements Session<Integer, Integer> {
            private final Session<InetSocketAddress, Bytestring> session;

            private TcpTestSession(Session<InetSocketAddress, Bytestring> session) {
                this.session = session;
            }

            @Override
            public boolean closed() throws InterruptedException {
                return session.closed();
            }

            @Override
            public Peer<Integer, Integer> peer() {
                return new TcpTestPeer(session.peer());
            }

            @Override
            public boolean send(Integer integer) throws InterruptedException {

                return session.send(new Bytestring(ByteBuffer.allocate(4).putInt(integer).array()));
            }

            @Override
            public void close() throws InterruptedException {
                session.close();
            }
        }

        private class TcpTestListener implements Listener<InetSocketAddress, Bytestring> {
            private final Listener<Integer, Integer> inner;

            private TcpTestListener(
                    Listener<Integer, Integer> inner) {

                this.inner = inner;
            }

            @Override
            public Send<Bytestring> newSession(Session<InetSocketAddress, Bytestring> session)
                    throws InterruptedException {

                return new TcpTestReceiver(inner.newSession(new TcpTestSession(session)));
            }
        }

        private class TcpTestConnection implements Connection<Integer, Integer> {
            private final Connection<InetSocketAddress, Bytestring> conn;

            private TcpTestConnection(Connection<InetSocketAddress, Bytestring> conn) {
                this.conn = conn;
            }

            @Override
            public Integer identity() {
                return me;
            }

            @Override
            public void close() throws InterruptedException {
                conn.close();
            }
        }

        @Override
        public Peer<Integer, Integer> getPeer(Integer you) {
            try {
                int port = ports[you];

                Peer<InetSocketAddress, Bytestring> p =
                        tcp.getPeer(new InetSocketAddress(InetAddress.getLocalHost(), port));

                if (p == null) return null;
                return new TcpTestPeer(p);

            } catch (UnknownHostException e) {
                return null;
            }
        }

        @Override
        public Connection<Integer, Integer> open(Listener<Integer, Integer> listener) {
            return new TcpTestConnection(tcp.open(new TcpTestListener(listener)));
        }
    }

    private static class TcpTestReceiver implements Send<Bytestring> {
        private final Send<Integer> inner;
        int last = 0;
        int i = 0;
        boolean closed = false;

        private TcpTestReceiver(Send<Integer> inner) {
            this.inner = inner;
        }

        @Override
        public boolean send(Bytestring bytestring) throws InterruptedException {
            if (closed) return false;

            byte[] bytes = bytestring.bytes;

            for (byte b : bytes) {
                last = (last << 8) + b;
                i ++;
                if (i == 4) {
                    i = 0;
                    if (!inner.send(last)) {
                        closed = true;
                        return false;
                    }
                    last = 0;
                }
            }

            return true;
        }

        @Override
        public void close() throws InterruptedException {
            inner.close();
            closed = true;
        }
    }

    private static class IntegerTestReceiver implements Send<Integer> {
        private final Send<Integer>[] send;
        private final Chan<Session<Integer, Integer>> sch;
        private final Session<Integer, Integer> s;
        private final int to;
        private int from = -1;
        private boolean closed = false;

        private IntegerTestReceiver(Integer to, Send<Integer>[] send, Chan<Session<Integer, Integer>> sch, Session<Integer, Integer> s) {
            this.to = to;
            this.send = send;
            this.sch = sch;
            this.s = s;
        }

        private IntegerTestReceiver(Integer to, int from, Send<Integer>[] send) {
            this.to = to;
            this.send = send;
            this.from = from;
            sch = null;
            s = null;
        }

        @Override
        public boolean send(Integer i) throws InterruptedException {
            if (closed) return false;

            if (from == -1) {
                if (!(i >= 0 && i < 4)) {
                    throw new InterruptedException();
                }
                from = i;
                return sch.send(s);
            } else {
                Assert.assertNotNull(send[i]);

                return send[i].send(i);
            }
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static class TestListener implements Listener<Integer, Integer> {
        private final Send<Integer>[] senders;
        private final Chan<Session<Integer, Integer>> sch;
        private final int me;

        private TestListener(
                Integer me,
                Send<Integer>[] senders,
                Chan<Session<Integer, Integer>> sch) {

            this.me = me;
            this.sch = sch;
            this.senders = senders;
        }

        @Override
        public Send<Integer> newSession(Session<Integer, Integer> session)
                throws InterruptedException {

            return new IntegerTestReceiver(me, senders, sch, session);
        }
    }

    @Before
    public void setup() throws InterruptedException, UnknownHostException {
        int[] numbers = new int[]{0, 1, 2};
        int[] port = new int[]{9997, 9998, 9999};
        InetSocketAddress[] addresses = new InetSocketAddress[3];

        for (int i : numbers) {
            addresses[i] = new InetSocketAddress(InetAddress.getLocalHost(), port[i]);
        }

        channels = new TcpTestChannel[3];
        listen = new TestListener[3];
        conn = (Connection<Integer, Integer>[]) new Connection[3];
        peer = (Peer<Integer, Integer>[][]) new Peer[3][3];
        session = (Session<Integer, Integer>[][]) new Session[3][3];
        rec = (Receive<Integer>[][]) new Receive[3][3];

        // Channels for receiving messages [from][to]
        Chan<Integer> chan[][] = (Chan<Integer>[][]) new Chan[3][3];

        // The executor for running all the threads.
        // Need one thread for each listener and connection endpoint, so 9 total.
        Executor exec = Executors.newFixedThreadPool(9);

        // Channel for sending new sessions made my remote peers. A vector.
        Chan<Session<Integer, Integer>>[] sch = new Chan[3];

        // Create objects.
        for (int i : numbers) {

            // create channels.
            for (int j : numbers) {

                // A channel cannot make a connection to itself, so we leave the
                // diagonal null.
                if (i != j) {
                    chan[i][j] = new BasicChan<>(2);
                    rec[i][j] = chan[i][j];
                } else {
                    chan[i][j] = null;
                    rec[i][j] = null;
                }
            }

            sch[i] = new BasicChan<>();

            // create listeners.
            listen[i] = new TestListener(i, chan[i], sch[i]);

            channels[i] = new TcpTestChannel(i, port, exec);
        }

        // create peers. This is really cool because you can pretty much
        // use the shape of the tensor to check the correctness of the program.
        for (int j : numbers) {
            for (int k : numbers) {

                peer[j][k] = channels[j].getPeer(k);

                if (j != k) {
                    Assert.assertNotNull(peer[j][k]);
                } else {
                    Assert.assertNull(peer[j][k]);
                }
            }
        }

        // Open channels objects.
        for (int i : numbers) {

            // three threads down.
            conn[i] = channels[i].open(listen[i]);
        }

        // Let's sleep for three seconds!
        Thread.sleep(3000);

        // create sessions
        for (int j : numbers) {
            int k = (j + 1)%3;

            session[j][k] = peer[j][k].openSession(new IntegerTestReceiver(k, j, chan[k]));

            Assert.assertNotNull(session[j][k]);
            Assert.assertTrue(!session[j][k].closed());

            session[k][j] = sch[k].receive();
            Assert.assertNotNull(session[k][j]);
            Assert.assertTrue(!session[k][j].closed());
        }


    }

    @After
    public void shutdown() throws InterruptedException {
        int[] numbers = new int[]{0, 1, 2};

        // Close sessions.
        for (int j : numbers) {
            for (int k : numbers) {
                if (j != k) {
                    session[j][k].close();
                    Assert.assertTrue(session[j][k].closed());
                }
            }
        }

        // Close connections.
        for (int i : numbers) {
            conn[i].close();
        }
    }

    @Test
    public void testOnAndOff() {

    }
}
