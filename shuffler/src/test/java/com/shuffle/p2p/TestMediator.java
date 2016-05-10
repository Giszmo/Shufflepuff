package com.shuffle.p2p;

import com.shuffle.chan.BasicChan;
import com.shuffle.chan.Chan;
import com.shuffle.chan.Receive;
import com.shuffle.chan.Send;
import com.shuffle.mock.MockChannel;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by Daniel Krawisz on 5/3/16.
 *
 * Why is this file written like this? Java often has constructs in which one defines a variable
 * with [type] [name]. I've been tending to write code with very long type names, so I had
 * been attempting to see if I could format the code in a way more adapted to very long typenames.
 *
 * In this code, I have attempted to align variable declarations so as to be aligned along the
 * division between type name and variable name.
 *
 */
                                                        public class TestMediator {

 Map<Integer, MockChannel<Integer, Mediator.Envelope<String, Integer>>> hosts;
  Map<Integer, Connection<Integer, Mediator.Envelope<String, Integer>>> mockConn;
                                                   Map<Integer, String> names;
           Map<String, MediatorClientChannel<String, Integer, Integer>> clients;
                               Map<String, Connection<String, Integer>> conn;
                                Map<String, Map<String, Chan<Integer>>> msgs;
                     Map<String, Map<String, Session<String, Integer>>> sessions;

                                     Mediator<String, Integer, Integer> mediator;

                                                            private Chan<Integer> getChan(String from, String to) {

                                     Map<String, Chan<Integer>> r = msgs.get(from);
                                                                if (r == null) {
                                                                    r = new HashMap<>();
                                                                    msgs.put(from, r);
                                                                }

                                                  Chan<Integer> ch = r.get(to);
                                                                if (ch == null) {
                                                                    ch = new BasicChan<>(1);
                                                                    r.put(to, ch);
                                                                }

                                                                return ch;
                                                            }

                                                            class TestListener implements Listener<String, Integer> {

                                                       private final String me;
                        private final Map<String, Session<String, Integer>> openSessions;

                                                                TestListener(
                                                                 String me,
                                  Map<String, Session<String, Integer>> openSessions
                                                                ) {

                                                                    this.me = me;
                                                                    this.openSessions = openSessions;
                                                                }

                                                                @Override
                                           public Send<Integer> newSession(Session<String, Integer> session)
                                                                        throws InterruptedException {

                                                             String name = session.peer().identity();
                                                                    openSessions.put(name, session);

                                                                    return getChan(me, name);
                                                                }
                                                            }

                                                            @Before
                                                public void setup() throws InterruptedException {
                                                                hosts     = new HashMap<>();
                                                                names     = new HashMap<>();
                                                                clients   = new HashMap<>();
                                                                sessions  = new HashMap<>();
                                                                conn      = new HashMap<>();
                                                                msgs      = new HashMap<>();
                                                                mockConn  = new HashMap<>();

                                                                // Three peers and one mediator.
                                                                for (int i = 0; i < 4; i ++) {
                                                                    hosts.put(i, new MockChannel<>(i, hosts));
                                                                }

                                                                names.put(1, "Moe");
                                                                names.put(2, "Larry");
                                                                names.put(3, "Curly");

                                                                mediator = new Mediator<>(hosts.get(0));

                                                                // Now connect the clients to the mediator server.
                                                                for (int i = 1; i < 4; i ++ ) {

                                                             String name = names.get(i);

           MockChannel<Integer, Mediator.Envelope<String, Integer>> host = hosts.get(i);

                                                                    // Make a new MediatorClientChannel using the MockChannel connected
                                                                    // to the Mediator.
                    MediatorClientChannel<String, Integer, Integer> client = new MediatorClientChannel<>(names.get(i), host.getPeer(0));

                                                                    clients.put(name, client);
                              Map<String, Session<String, Integer>> openSessions = new HashMap<>();
                                                                    // Fill sessions with empty map.
                                                                    sessions.put(name, openSessions);

                                                                    // Open client.
            Connection<Integer, Mediator.Envelope<String, Integer>> mock = host.open(
                                                                            new Listener<Integer, Mediator.Envelope<String, Integer>>(){
                                                                                // The listener doesn't need to do anything because the
                                                                                // mediator doesn't initiate connections.
                                                                                @Override
                                public Send<Mediator.Envelope<String, Integer>> newSession(Session<Integer, Mediator.Envelope<String, Integer>> session) throws InterruptedException {
                                                                                    if (session == null) throw new NullPointerException();
                                                                                    throw new IllegalArgumentException();
                                                                                }
                                                                            });

                                                                    Assert.assertNotNull(mock);

                                                                    mockConn.put(i, mock);

                                        Connection<String, Integer> connection = client.open(new TestListener(name, openSessions));

                                                                    Assert.assertTrue(connection != null);

                                                                    conn.put(name, connection);
                                                                }

                                                            }

                                                            @Test
                                                public void TestOnAndOff() {

                                                            }

                                                public void openSessionFail(String from, String to) throws InterruptedException {

                                                                try {
                                                                    // Open session.
                                           Session<String, Integer> ss = clients.get(from).getPeer(to).openSession(getChan(from, to));

                                                                    Assert.assertNull(ss);

                                                                } catch (NullPointerException | IllegalArgumentException e) {
                                                                    Assert.fail("Null pointer intercepted.");
                                                                }
                                                            }

                                                            private class NetworkEdge {
                                                                private final String a, b;

                                                                public final Session<String, Integer> ab;
                                                                public final Session<String, Integer> ba;

                                                                private NetworkEdge(String a, String b) throws InterruptedException {

                                                                    if (a == null || b == null) {
                                                                        throw new NullPointerException();
                                                                    }

                                                                    this.a = a;
                                                                    this.b = b;

                                                                    // Open session.
                                                                    ab = clients.get(a).getPeer(b).openSession(getChan(a, b));

                                                                    if (ab == null) {
                                                                        Assert.fail("Null session returned; expected success.");
                                                                        throw new IllegalArgumentException();
                                                                    }

                                                                    // Session should be open.
                                                                    if (ab.closed()) {
                                                                        Assert.fail("Session is closed.");
                                                                        throw new IllegalArgumentException();
                                                                    }

                                                                    // Session should exist in openSessions.
                                                                    ba = sessions.get(b).get(a);
                                                                    if (ba == null) {
                                                                        Assert.fail("Null session found.");
                                                                        Assert.fail();
                                                                    }
                                                                    Assert.assertFalse(ba.closed());
                                                                }
                                                            }

                                                            @Test
                                                            public void TestOpenSessions() throws InterruptedException {

                                                                // There is no shemp, so a session to him cannot be opened.
                                                                openSessionFail("Moe", "Shemp");

                                                                // Moe opens a session to Larry.
                                                                new NetworkEdge("Moe", "Larry");

                                                                openSessionFail("Moe", "Larry");

                                                                openSessionFail("Larry", "Moe");

                                                                new NetworkEdge("Moe", "Curly");

                                                                new NetworkEdge("Curly", "Larry");
                                                            }

                                                            Integer msgNo = 73;

                                               private void sendMessage(String a, String b, Session<String, Integer> ab) throws InterruptedException {

                                                                // Send a message from A to B.

                                                                System.out.println("About to send message from " + a + " to " + b);
                                                                ab.send(++msgNo);
                                                                Assert.assertEquals(msgNo, getChan(b, a).receive());


                                                            }

                                                public void sendMessages(NetworkEdge ab) throws InterruptedException {
                                                                sendMessage(ab.a, ab.b, ab.ab);
                                                                sendMessage(ab.b, ab.a, ab.ba);

                                                            }

                                                            @Test
                                                            public void TestSendMessage() throws InterruptedException {
                                                                NetworkEdge ml = new NetworkEdge("Moe", "Larry");

                                                                NetworkEdge mc = new NetworkEdge("Moe", "Curly");

                                                                sendMessages(ml);

                                                                sendMessages(mc);
                                                            }

                                                            @After
                                                            public void shutdown() throws InterruptedException {
                                                                // close all sessions.
                                                                for (Map<String, Session<String, Integer>> ss : sessions.values())
                                                                    for (Session<String, Integer> s : ss.values()) s.close();

                                                                // close connections to server.
                                                                for (Connection<String, Integer> client : conn.values()) {
                                                                    client.close();
                                                                }

                                                                // close underlying mock connections.
                                                                for (Connection<Integer, Mediator.Envelope<String, Integer>> m : mockConn.values()) {
                                                                    m.close();
                                                                }

                                                                // close mediator server.
                                                                mediator.close();

                                                                // prevent memory leak.
                                                                hosts.clear();
                                                            }
                                                        }
