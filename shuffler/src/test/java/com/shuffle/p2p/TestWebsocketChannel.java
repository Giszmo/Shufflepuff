/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;

import com.shuffle.chan.Send;

import org.glassfish.tyrus.core.TyrusSession;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;

import javax.websocket.DeploymentException;

/**
 * Created by Eugene Siegel on 4/28/16.
 */

public class TestWebsocketChannel {

    WebsocketServerChannel server;
    WebsocketClientChannel client;
    Connection<InetAddress, Bytestring> serverConn;
    Connection<URI, Bytestring> clientConn;
    Session<InetAddress, Bytestring> serverSession;
    Session<URI, Bytestring> clientSession;
    String serverMessage;
    String clientMessage;
    Session<InetAddress, Bytestring> currentSession;
    Send<Bytestring> clientReceiverTest;

    @Before
    public void setup() throws UnknownHostException {

        client = new WebsocketClientChannel();
        server = new WebsocketServerChannel(8025, "localhost", InetAddress.getLocalHost());

        Listener<InetAddress, Bytestring> serverListener = new Listener<InetAddress, Bytestring>() {
            @Override
            public Send<Bytestring> newSession(com.shuffle.p2p.Session<InetAddress, Bytestring> session) throws InterruptedException {

                /**
                 *  Since newSession is only called once in the test, we can store the session in
                 *  currentSession (we don't have to use a HashMap).
                 */
                currentSession = session;

                return new Send<Bytestring>() {

                    @Override
                    public boolean send(Bytestring bytestring) throws InterruptedException {
                        TestWebsocketChannel.this.serverMessage = new String(bytestring.bytes);
                        return true;
                    }

                    @Override
                    public void close() {

                    }
                };
            }
        };

        final Send<Bytestring> clientReceiver = new Send<Bytestring>() {
            @Override
            public boolean send(Bytestring bytestring) throws InterruptedException {
                TestWebsocketChannel.this.clientMessage = new String(bytestring.bytes);
                return true;
            }

            @Override
            public void close() {

            }
        };

        clientReceiverTest = clientReceiver;

        Listener<URI, Bytestring> clientListener = new Listener<URI, Bytestring>() {
            @Override
            public Send<Bytestring> newSession(Session<URI, Bytestring> session) throws InterruptedException {
                return clientReceiver;
            }
        };

        serverConn = server.open(serverListener);

        // must call 'open' with client before clientSession can call 'close'
        clientConn = client.open(clientListener);

    }

    @Test
    public void sendAndReceive() throws InterruptedException, URISyntaxException {
        Peer<URI, Bytestring> peer = client.getPeer(new URI("ws://localhost:8025"));
        clientSession = peer.openSession(clientReceiverTest);

        Assert.assertNotNull(clientSession);
        Assert.assertTrue(!clientSession.closed());

        String message = "Shufflepuff test";
        Bytestring bytestring = new Bytestring(message.getBytes());
        Boolean clientSent = clientSession.send(bytestring);
        Assert.assertTrue(clientSent);

        serverSession = currentSession;

        Assert.assertNotNull(serverSession);
        Assert.assertTrue(!serverSession.closed());

        String message2 = "houston, we have a problem";
        Bytestring bytestring2 = new Bytestring(message2.getBytes());
        Boolean serverSent = serverSession.send(bytestring2);
        Assert.assertTrue(serverSent);

        // Sleep because it takes (miniscule) time to receive a message.
        Thread.sleep(2000);
        Assert.assertEquals(message, this.serverMessage); // the server receives this message
        Assert.assertEquals(message2, this.clientMessage); // the client receives this message
    }

    @After
    public void shutdown() {
        serverSession.close();
        clientSession.close();
        serverConn.close();
        clientConn.close();
        Assert.assertTrue(serverSession.closed());
        Assert.assertTrue(clientSession.closed());
    }

}
