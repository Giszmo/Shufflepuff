/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;

import junit.framework.Assert;

import org.glassfish.tyrus.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.shuffle.chan.BasicChan;
import com.shuffle.chan.Chan;
import com.shuffle.chan.ReceiveChan;
import com.shuffle.chan.SendChan;
import com.shuffle.p2p.WebsocketClientChannel;
import com.shuffle.p2p.WebsocketServerChannel;
import com.shuffle.player.Connect;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;

import javax.websocket.DeploymentException;

/**
 * Tests for the websocket connection
 *
 * Created by Eugene Siegel on 4/26/16.
 */

public class TestWebsocketChannel {

    Connection<Integer, Integer> conn;

    Session<Integer, Integer> session;

    WebsocketServerChannel channel2;

    /*
    private static class WebsocketTestClient implements Channel<Integer, Integer> {

        private final WebsocketClientChannel client;
        private final WebsocketClientChannel.WebsocketPeer peer;
        private final Integer me;

        WebsocketTestClient(Integer me) throws URISyntaxException {
            this.me = me;
            this.client = new WebsocketClientChannel();
            this.peer = new WebsocketClientChannel().new WebsocketPeer(new URI("ws://localhost:8080"));
        }

        private class WebsocketTestPeer implements Peer<Integer, Integer> {

            private final Peer<URI, Bytestring> peer;

            private WebsocketTestPeer(Peer<URI, Bytestring> peer) {
                this.peer = peer;
            }

            @Override
            public Integer identity() {
                return 8080;
            }

            @Override
            public Session<Integer, Integer> openSession(Receiver<Integer> receiver) throws InterruptedException {

                Session<URI, Bytestring> p = peer.openSession(new WebsocketTestReceiver(receiver));

                if (p == null) {
                    return null;
                }

                Session<Integer, Integer> session = new WebsocketTestSession(p);

                session.send(me);

                return session;
            }

            @Override
            public boolean open() {
                return peer.open();
            }

            @Override
            public void close() {
                peer.close();
            }
        }

        private class WebsocketTestSession implements Session<Integer, Integer> {

            private final Session<URI, Bytestring> session;

            private WebsocketTestSession(Session<URI, Bytestring> session) {
                this.session = session;
            }

            @Override
            public boolean closed() {
                return session.closed();
            }

            @Override
            public Peer<Integer, Integer> peer() {
                return new WebsocketTestPeer(session.peer());
            }

            @Override
            public boolean send(Integer integer) throws InterruptedException {
                return session.send(new Bytestring(ByteBuffer.allocate(4).putInt(integer).array()));
            }

            @Override
            public void close() {
                session.close();
            }
        }

        private class WebsocketTestListener implements Listener<URI, Bytestring> {

            private final Listener<Integer, Integer> inner;

            private WebsocketTestListener(Listener<Integer, Integer> inner) {
                this.inner = inner;
            }

            @Override
            public Receiver<Bytestring> newSession(Session<URI, Bytestring> session) throws InterruptedException {
                return new WebsocketTestReceiver(inner.newSession(new WebsocketTestSession(session)));
            }
        }

        private class WebsocketTestConnection implements Connection<Integer, Integer> {

            private final Connection<URI, Bytestring> conn;

            private WebsocketTestConnection(Connection<URI, Bytestring> conn) {
                this.conn = conn;
            }

            @Override
            public Integer identity() {
                return me;
            }

            @Override
            public void close() {
                conn.close();
            }
        }

        @Override
        public Peer<Integer, Integer> getPeer(Integer you) {
            try {

                Peer<URI, Bytestring> p = client.getPeer(new URI("ws://localhost:8080"));

                if (p == null) return null;
                return new WebsocketTestPeer(p);

            } catch (URISyntaxException e) {
                return null;
            }
        }

        @Override
        public Connection<Integer, Integer> open(Listener<Integer, Integer> listener) {
            return new WebsocketTestConnection(client.open(new WebsocketTestListener(listener)));
        }
    }*/


    private static class WebsocketTestChannel implements Channel<Integer, Integer> {

        private final WebsocketServerChannel server;
        private final Integer me;
        private final int[] ports;

        WebsocketTestChannel(Integer me, int[] ports) throws UnknownHostException {
            this.me = me;
            this.ports = ports;
            this.server = new WebsocketServerChannel(ports[me], "localhost", InetAddress.getLocalHost());
        }

        private class WebsocketTestPeer implements Peer<Integer, Integer> {

            private final Peer<InetAddress, Bytestring> peer;

            private WebsocketTestPeer(Peer<InetAddress, Bytestring> peer) {
                this.peer = peer;
            }

            @Override
            public Integer identity() {
                return ports[me];
            }

            @Override
            public Session<Integer, Integer> openSession(Receiver<Integer> receiver) throws InterruptedException {

                Session<InetAddress, Bytestring> p = peer.openSession(new WebsocketTestReceiver(receiver));

                if (p == null) {
                    return null;
                }

                Session<Integer, Integer> session = new WebsocketTestSession(p);

                // Tell them who we are
                session.send(me);

                return session;
            }

            @Override
            public boolean open() {
                return peer.open();
            }

            @Override
            public void close() {
                peer.close();
            }
        }

        private class WebsocketTestSession implements Session<Integer, Integer> {

            private final Session<InetAddress, Bytestring> session;

            private WebsocketTestSession(Session<InetAddress, Bytestring> session) {
                this.session = session;
            }

            @Override
            public boolean closed() {
                return session.closed();
            }

            @Override
            public Peer<Integer, Integer> peer() {
                return new WebsocketTestPeer(session.peer());
            }

            @Override
            public boolean send(Integer integer) throws InterruptedException {
                return session.send(new Bytestring(ByteBuffer.allocate(4).putInt(integer).array()));
            }

            @Override
            public void close() {
                session.close();
            }
        }

        private class WebsocketTestListener implements Listener<InetAddress, Bytestring> {

            private final Listener<Integer, Integer> inner;

            private WebsocketTestListener(Listener<Integer, Integer> inner) {
                this.inner = inner;
            }

            @Override
            public Receiver<Bytestring> newSession(Session<InetAddress, Bytestring> session) throws InterruptedException {
                return new WebsocketTestReceiver(inner.newSession(new WebsocketTestSession(session)));
            }
        }

        private class WebsocketTestConnection implements Connection<Integer, Integer> {

            private final Connection<InetAddress, Bytestring> conn;

            private WebsocketTestConnection(Connection<InetAddress, Bytestring> conn) {
                this.conn = conn;
            }

            @Override
            public Integer identity() {
                return me;
            }

            @Override
            public void close() {
                conn.close();
            }
        }

        @Override
        public Peer<Integer, Integer> getPeer(Integer you) {
            try {
                int port = ports[you];

                // this is messed up because both client and server are localhost...
                Peer<InetAddress, Bytestring> p = server.getPeer(InetAddress.getLocalHost());

                if (p == null) return null;
                return new WebsocketTestPeer(p);

            } catch (UnknownHostException e) {
                return null;
            }
        }

        @Override
        public Connection<Integer, Integer> open(Listener<Integer, Integer> listener) {
            return new WebsocketTestConnection(server.open(new WebsocketTestListener(listener)));
        }

    }

    private static class WebsocketTestReceiver implements Receiver<Bytestring> {

        private final Receiver<Integer> inner;
        int last = 0;
        int i = 0;

        private WebsocketTestReceiver(Receiver<Integer> inner) {
            this.inner = inner;
        }

        @Override
        public void receive(Bytestring bytestring) throws InterruptedException {
            byte[] bytes = bytestring.bytes;

            for (byte b : bytes) {
                last = (last << 8) + b;
                i ++;
                if (i == 4) {
                    i = 0;
                    inner.receive(last);
                    last = 0;
                }
            }
        }
    }

    private static class IntegerTestReceiver implements Receiver<Integer> {

        private final SendChan<Integer>[] send;
        private final Chan<Session<Integer, Integer>> sch;
        private final Session<Integer, Integer> s;
        private final int to;
        private int from = -1;

        private IntegerTestReceiver(Integer to, SendChan<Integer>[] send, Chan<Session<Integer, Integer>> sch, Session<Integer, Integer> s) {
            this.to = to;
            this.send = send;
            this.sch = sch;
            this.s = s;
        }

        private IntegerTestReceiver(Integer to, int from, SendChan<Integer>[] send) {
            this.to = to;
            this.send = send;
            this.from = from;
            sch = null;
            s = null;
        }

        @Override
        public void receive(Integer i) throws InterruptedException {

            if (from == -1) {
                if (!(i >= 0 && i < 4)) {
                    throw new InterruptedException();
                }
                from = i;
                sch.send(s);
            } else {
                Assert.assertNotNull(send[i]);

                send[i].send(i);
            }
        }
    }

    private static class TestListener implements Listener<Integer, Integer> {

        private final SendChan<Integer>[] senders;
        private final Chan<Session<Integer, Integer>> sch;
        private final int me;

        private TestListener(Integer me, SendChan<Integer>[] senders, Chan<Session<Integer, Integer>> sch) {
            this.me = me;
            this.sch = sch;
            this.senders = senders;
        }

        @Override
        public Receiver<Integer> newSession(Session<Integer, Integer> session) throws InterruptedException {
            return new IntegerTestReceiver(me, senders, sch, session);
        }
    }

    public class temp implements Runnable {

        public temp() {}

        public void run() {
            try {
                WebsocketClientChannel client = new WebsocketClientChannel();
                WebsocketClientChannel.WebsocketPeer peer3 = client.new WebsocketPeer(new URI("ws://localhost:8080"));
                WebsocketClientChannel.WebsocketPeer.WebsocketSession session4 = peer3.newSession();
                String message = "test message";
                //Bytestring bytestring = new Bytestring(message.getBytes());
                //session4.send(bytestring);
                session4.session.getBasicRemote().sendText(message);
                Assert.assertTrue(session4.session.isOpen());
                Assert.assertTrue(!session4.closed());
                Thread.sleep(2000);
                Assert.assertEquals(client.globalMessage, message);
            } catch (Exception e) {

            }
        }
    }

    @Before
    public void setup() throws InterruptedException, UnknownHostException, URISyntaxException, DeploymentException, IOException {
        int[] numbers = new int[]{0};
        int[] port = new int[]{8080};
        InetAddress[] addresses = new InetAddress[1];

        for (int i : numbers) {
            addresses[i] = InetAddress.getLocalHost();
        }

        Chan<Integer> chan[][] = (Chan<Integer>[][]) new Chan[1][1];
        chan[0][0] = new BasicChan<>(2);

        Chan<Session<Integer, Integer>>[] sch = new Chan[1];
        sch[0] = new BasicChan<>();

        TestListener listen = new TestListener(0, chan[0], sch[0]);
        //WebsocketTestChannel channel = new WebsocketTestChannel(0, port);

        //WebsocketTestClient client = new WebsocketTestClient(8080);

        //conn = channel.open(listen);

        //Server server = new Server("localhost", 8080, "", new HashMap<String, Object>(), WebsocketServerChannel.WebsocketServerEndpoint.class);
        //server.start();

        channel2 = new WebsocketServerChannel(8080, "localhost", InetAddress.getLocalHost());
        Listener<InetAddress, Bytestring> listener2 = new Listener<InetAddress, Bytestring>() {
            @Override
            public Receiver<Bytestring> newSession(Session<InetAddress, Bytestring> session) throws InterruptedException {
                return new Receiver<Bytestring>() {
                    @Override
                    public void receive(Bytestring bytestring) throws InterruptedException {
                        return;
                    }
                };
            }
        };
        channel2.open(listener2);

        final temp tt = new temp();
        Thread t1 = new Thread(tt);
        t1.start();

        Thread.sleep(5000);

        /*
        WebsocketClientChannel.WebsocketPeer peer3 = new WebsocketClientChannel().new WebsocketPeer(new URI("ws://localhost:8080"));
        WebsocketClientChannel.WebsocketPeer.WebsocketSession session4 = peer3.newSession();
        String message = "test message";
        Bytestring bytestring = new Bytestring(message.getBytes());
        session4.send(bytestring);
        Assert.assertTrue(session4.session.isOpen());
        Assert.assertTrue(!session4.closed());
        */
        //Assert.assertNotNull(channel.server.globalListener);
        //Assert.assertNotNull(channel.server.globalReceiver);

        Assert.assertNotNull(channel2.globalListener);
        Assert.assertNotNull(channel2.globalReceiver);

        Thread.sleep(8000);
        //Assert.assertNotNull(conn);

    }

    @After
    public void shutdown() {
        //conn.close();
    }

    @Test
    public void testOnAndOff() {

    }

}
