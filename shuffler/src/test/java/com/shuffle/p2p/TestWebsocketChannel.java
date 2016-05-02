/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;

import org.glassfish.tyrus.core.TyrusSession;
import org.glassfish.tyrus.server.Server;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.MessageHandler;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerEndpoint;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Created by Eugene Siegel on 4/28/16.
 */

public class TestWebsocketChannel {

    @Before
    public void setup() throws UnknownHostException, URISyntaxException, DeploymentException, InterruptedException {
        WebsocketClientChannel client = new WebsocketClientChannel();
        WebsocketServerChannel server = new WebsocketServerChannel(8080, "localhost", InetAddress.getLocalHost());

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

        server.open(serverListener);

        WebsocketClientChannel.WebsocketPeer peer = new WebsocketClientChannel().new WebsocketPeer(new URI("ws://localhost:8080"));
        WebsocketClientChannel.WebsocketPeer.WebsocketSession clientSession = peer.newSession();
        String message = "Shufflepuff test";
        Bytestring bytestring = new Bytestring(message.getBytes());
        clientSession.send(bytestring);
    }

    @After
    public void shutdown() {

    }

    @Test
    public void testOnAndOff() {

    }

}
