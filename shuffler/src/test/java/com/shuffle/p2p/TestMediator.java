package com.shuffle.p2p;

import com.shuffle.chan.BasicChan;
import com.shuffle.chan.Chan;
import com.shuffle.chan.Send;
import com.shuffle.mock.MockChannel;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Daniel Krawisz on 5/3/16.
 */
                                                        public class TestMediator {

 Map<Integer, MockChannel<Integer, Mediator.Envelope<String, Integer>>> hosts;
  Map<Integer, Connection<Integer, Mediator.Envelope<String, Integer>>> mockConn;
                                                   Map<Integer, String> names;
           Map<String, MediatorClientChannel<String, Integer, Integer>> clients;
                               Map<String, Connection<String, Integer>> conn;
                                Map<String, Map<String, Chan<Integer>>> receivers;
                     Map<String, Map<String, Session<String, Integer>>> sessions;

                                     Mediator<String, Integer, Integer> mediator;

                                                            class TestSend implements Send<Integer> {

                                               private final Send<Integer> ch;

                                                                TestSend(String from, String to) {

                                         Map<String, Chan<Integer>> r = receivers.get(from);
                                                                    if (r == null) throw new IllegalArgumentException();

                                                      Chan<Integer> ch = r.get(to);
                                                                    if (ch == null) throw new IllegalArgumentException();

                                                                    this.ch = ch;
                                                                }

                                                                @Override
                                                 public boolean send(Integer integer) throws InterruptedException {
                                                                    return ch.send(integer);
                                                                }

                                                                @Override
                                                    public void close() throws InterruptedException {
                                                                    ch.close();
                                                                }
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

                                                                    return new TestSend(name, me);
                                                                }
                                                            }

                                                            @Before
                                                public void setup() throws InterruptedException {
                                                                hosts     = new HashMap<>();
                                                                names     = new HashMap<>();
                                                                clients   = new HashMap<>();
                                                                sessions  = new HashMap<>();
                                                                conn      = new HashMap<>();
                                                                receivers = new HashMap<>();
                                                                mockConn  = new HashMap<>();

                                                                // Three peers and one mediator.
                                                                for (int i = 0; i < 4; i ++) {
                                                                    hosts.put(i, new MockChannel<>(i, hosts));
                                                                }

                                                                names.put(1, "Moe");
                                                                names.put(2, "Larry");
                                                                names.put(3, "Curly");

                                                                // Set up receiver channels.
                                                                for (String from : names.values()) {

                                         Map<String, Chan<Integer>> receive = new HashMap<>();
                                                                    receivers.put(from, receive);

                                                                    for (String to : names.values())
                                                                        receive.put(to, new BasicChan<Integer>(7));
                                                                }

                                                                mediator = new Mediator<>(hosts.get(0));

                                                                for (int i = 1; i < 4; i ++ ) {

                                                             String name = names.get(i);

           MockChannel<Integer, Mediator.Envelope<String, Integer>> host = hosts.get(i);

                    MediatorClientChannel<String, Integer, Integer> client = new MediatorClientChannel<>(names.get(i), host.getPeer(0));

                                                                    clients.put(name, client);
                              Map<String, Session<String, Integer>> openSessions = new HashMap<>();
                                                                    sessions.put(name, openSessions);

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

                                                public void openSession(String from, String to) throws InterruptedException {

                                                                // Open session.
                                                                clients.get(from).getPeer(to).openSession(new TestSend(from, to));

                                                                // Session should exist in openSessions.
                                                                try {
                                                                    Assert.assertFalse(sessions.get(from).get(to).closed());
                                                                    Assert.assertFalse(sessions.get(to).get(from).closed());
                                                                } catch (NullPointerException e) {
                                                                    Assert.fail("Session not opened properly.");
                                                                }
                                                            }

                                                            @Test
                                                            public void TestOpenSessions() throws InterruptedException {
                                                                // Moe opens a session to Larry.
                                                                openSession("Moe", "Larry");



                                                                // Moe opens a session to Curly.
                                                            }

                                                            @Test
                                                            public void TestSendMessage() {

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
