/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import javax.websocket.DeploymentException;

/**
 * Created by Eugene Siegel on 4/28/16.
 */

public class TestWebsocketChannel {

    Connection<InetAddress, Bytestring> serverConn;
    Connection<URI, Bytestring> clientConn;
    WebsocketServerChannel.WebsocketPeer.WebsocketSession serverSession;
    WebsocketClientChannel.WebsocketPeer.WebsocketSession clientSession;

    @Before
    public void setup() throws UnknownHostException, URISyntaxException, DeploymentException, InterruptedException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        WebsocketClientChannel client = new WebsocketClientChannel();
        WebsocketServerChannel server = new WebsocketServerChannel(8025, "localhost", InetAddress.getLocalHost());

        Listener<InetAddress, Bytestring> serverListener = new Listener<InetAddress, Bytestring>() {
            @Override
            public Receiver<Bytestring> newSession(com.shuffle.p2p.Session<InetAddress, Bytestring> session) throws InterruptedException {
                return new Receiver<Bytestring>() {
                    @Override
                    public void receive(Bytestring bytestring) throws InterruptedException {
                        return;
                    }
                };
            }
        };

        Listener<URI, Bytestring> clientListener = new Listener<URI, Bytestring>() {
            @Override
            public Receiver<Bytestring> newSession(Session<URI, Bytestring> session) throws InterruptedException {
                return new Receiver<Bytestring>() {
                    @Override
                    public void receive(Bytestring bytestring) throws InterruptedException {
                        return;
                    }
                };
            }
        };

        serverConn = server.open(serverListener);

        // must call 'open' with client before clientSession can call 'close'
        clientConn = client.open(clientListener);

        Thread.sleep(2000);

        WebsocketClientChannel.WebsocketPeer peer = client.new WebsocketPeer(new URI("ws://localhost:8025"));
        clientSession = peer.newSession();
        Assert.assertTrue(!clientSession.closed());
        Assert.assertTrue(clientSession.session.isOpen());

        String message = "Shufflepuff test";
        Bytestring bytestring = new Bytestring(message.getBytes());
        Boolean clientSent = clientSession.send(bytestring);
        Assert.assertTrue(clientSent);

        serverSession = server.staticOpenSessions.get(InetAddress.getByName("127.0.0.1"));
        Assert.assertNotNull(serverSession);
        Boolean serverSent = serverSession.send(bytestring);
        Assert.assertTrue(serverSent);

    }

    @Test
    public void testOnAndOff() {

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
