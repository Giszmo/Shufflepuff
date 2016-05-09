/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.player.Messages;
import com.shuffle.mock.MockSessionIdentifier;
import com.shuffle.mock.MockSigningKey;
import com.shuffle.mock.MockVerificationKey;
import com.shuffle.player.MockNetwork;
import com.shuffle.protocol.blame.BlameException;
import com.shuffle.protocol.message.Packet;
import com.shuffle.protocol.message.Phase;
import com.shuffle.sim.NetworkSim;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Tests for certain functions pertaining to network interactions.
 *
 * Created by Daniel Krawisz on 12/7/15.
 */
public class TestMailbox {

    static class BroadcastTestCase {
        public final int recipients;
        public final int sender;

        public BroadcastTestCase(int recipients, int sender) {
            this.recipients = recipients;
            this.sender = sender;
        }
    }

    @Test
    public void testBroadcast() throws InvalidParticipantSetException, InterruptedException, IOException {
        BroadcastTestCase[] tests =
                new BroadcastTestCase[]{
                        new BroadcastTestCase(1, 1),
                        new BroadcastTestCase(2, 1),
                        new BroadcastTestCase(2, 2),
                        new BroadcastTestCase(3, 2),
                        new BroadcastTestCase(4, 2),
                };

        for (BroadcastTestCase test : tests) {

            // make the set of players.
            Set<VerificationKey> players = new HashSet<>();
            for (int i = 1; i <= test.recipients; i ++) {
                players.add(new MockVerificationKey(i));
            }

            MockSigningKey me = new MockSigningKey(test.sender);

            MockNetwork network = new MockNetwork(me);

            Messages messages = new Messages(new MockSessionIdentifier("test Broadcast"), me.VerificationKey(), network, network);

            new Mailbox(me.VerificationKey(), players, messages
            ).broadcast(messages.make(), Phase.Shuffling);

            for (Map.Entry<? extends Packet, VerificationKey> sent : network.getResponses()) {
                VerificationKey sentBy = sent.getValue();
                Assert.assertTrue(players.contains(sentBy));
                players.remove(sentBy);
            }

            Assert.assertEquals(1, players.size());

            players.remove(me.VerificationKey());
            Assert.assertEquals(0, players.size());
        }
    }

    static class SendToTestCase {
        public final int sender;
        public final int recipient;
        public final int players;
        public final boolean success;

        public SendToTestCase(int sender, int recipient, int players, boolean success) {
            this.sender = sender;
            this.recipient = recipient;
            this.players = players;
            this.success = success;
        }
    }

    @Test
    public void testSend() throws InvalidParticipantSetException, InterruptedException, IOException {
        SendToTestCase[] tests = new SendToTestCase[]{
                // Case where recipient does not exist.
                new SendToTestCase(1, 3, 2, false),
                // Cannot send to myself.
                new SendToTestCase(1, 1, 2, false),
                // Success case.
                new SendToTestCase(1, 2, 2, true)
        };

        int index = 0;
        for (SendToTestCase test : tests) {
            // The player sending and inbox.
            MockSigningKey sk = new MockSigningKey(1);

            // Create mock network object.
            MockNetwork network = new com.shuffle.player.MockNetwork(sk);

            // make the set of players.
            Set<VerificationKey> players = new HashSet<>();
            for (int j = 1; j <= test.players; j ++) {
                players.add(new MockVerificationKey(j));
            }

            // Set up the shuffle machine (only used to query for the current phase).
            Messages messages = new Messages(
                    new MockSessionIdentifier("testSend" + index),
                    sk.VerificationKey(), network, network);

            new Mailbox(
                    sk.VerificationKey(), players, messages
            ).send(messages.make().prepare(
                    Phase.Shuffling,
                    new MockVerificationKey(test.recipient)));

            int expected = (test.success ? 1 : 0);

            Queue<Map.Entry<com.shuffle.player.Packet, VerificationKey>> responses = network.getResponses();
            Assert.assertEquals(
                    String.format(
                            "Recieved %d responses when only expected %d in test case %d",
                            responses.size(), expected, index),
                    (test.success ? 1 : 0),
                    responses.size());

            for (Map.Entry msg : responses) {
                Assert.assertTrue("Received response does not equal expected",
                        new MockVerificationKey(test.recipient).equals(msg.getValue()));
            }
            index++;
        }
    }

    static class ReceiveFromTestCase {
        public final int[] players;
        public final int requested; // The player that the message was expected from.
        public final Phase phase; // The expected phase.
        public final Packet packet;
        public final boolean error; // If an exception is expected.

        public ReceiveFromTestCase(
                int[] players,
                int requested,
                Phase phase,
                Packet packet,
                boolean error
        ) {
            this.players = players;
            this.requested = requested;
            this.packet = packet;
            this.error = error;
            this.phase = phase;
        }
    }

    @Test(expected = WaitingException.class)
    public void testReceiveFrom()
            throws InvalidParticipantSetException, InterruptedException,
            BlameException, FormatException, WaitingException, IOException {

        ReceiveFromTestCase[] tests = new ReceiveFromTestCase[]{
                // time out exception test case.
                new ReceiveFromTestCase(
                        new int []{1,2,3}, 2, Phase.Shuffling, null, true
                ),
                // Various malformed inputs.
                // TODO
        };

        int index = 0;
        for (ReceiveFromTestCase test : tests) {
            // The player sending and inbox.
            MockSigningKey sk = new MockSigningKey(1);

            // make the set of players.
            Set<VerificationKey> players = new HashSet<>();
            for (int j = 1; j <= test.players.length; j ++) {
                players.add(new MockVerificationKey(test.players[j - 1]));
            }

            MockNetwork network = new MockNetwork(sk);

            new Mailbox(sk.VerificationKey(), players,
                    new Messages(
                            new MockSessionIdentifier("receiveFromTest" + index),
                            sk.VerificationKey(),
                            network, network)
            ).receiveFrom(new MockVerificationKey(test.requested), test.phase);
            index++;
        }
    }

    static class ReceiveFromMultipleTestCase {
        public final int players;
        public final int me;
        public final int[] receiveFrom; // Who are we expecting?
        public final int[] sendBefore; // Send before we call the function.
        public final int[] sendAfter; // Send after the function is called.
        public final boolean timeoutExpected;

        public ReceiveFromMultipleTestCase(
                int players,
                int me,
                int[] receiveFrom,
                int[] sendBefore,
                int[] sendAfter
        ) {
            this.players = players;
            this.me = me;
            this.receiveFrom = receiveFrom;
            this.sendBefore = sendBefore;
            this.sendAfter = sendAfter;
            timeoutExpected = false;
        }

        public ReceiveFromMultipleTestCase(
                int players,
                int me,
                int[] receiveFrom,
                int[] sendBefore,
                int[] sendAfter,
                boolean timeoutExpected
        ) {
            this.players = players;
            this.me = me;
            this.receiveFrom = receiveFrom;
            this.sendBefore = sendBefore;
            this.sendAfter = sendAfter;
            this.timeoutExpected = timeoutExpected;
        }
    }

    @Test
    public void testReceiveFromMultiple()
            throws InvalidParticipantSetException, InterruptedException, BlameException,
            FormatException, IOException {

        ReceiveFromMultipleTestCase[] tests = new ReceiveFromMultipleTestCase[]{
                // Very simple test case.
                new ReceiveFromMultipleTestCase(3, 2,
                        new int[]{3},
                        new int[]{},
                        new int[]{3}),
                // Timeout expected.
                new ReceiveFromMultipleTestCase(4, 2,
                        new int[]{3, 4},
                        new int[]{},
                        new int[]{3}, true),
                // before and after.
                new ReceiveFromMultipleTestCase(4, 2,
                        new int[]{3, 4},
                        new int[]{4},
                        new int[]{3})
        };

        int i = 0;
        for (ReceiveFromMultipleTestCase test : tests) {
            System.out.println("receive from multiple test case " + i + " me = " + test.me);

            // The player sending and inbox.
            MockSigningKey sk = new MockSigningKey(test.me);

            // Create mock network object.
            MockNetwork network = new MockNetwork(sk);

            MockSessionIdentifier mockSessionIdentifier
                    = new MockSessionIdentifier("receiveFromMultiple" + i);

            Map<SigningKey, Messages> messages = new HashMap<>();

            // make the set of players.
            Set<VerificationKey> players = new HashSet<>();
            for (int j = 1; j <= test.players; j ++) {
                MockSigningKey player = new MockSigningKey(j);
                players.add(player.VerificationKey());

                messages.put(player, new Messages(mockSessionIdentifier, player.VerificationKey(), network, network));
            }

            // Set up the network operations object.
            Mailbox mailbox = new Mailbox(sk.VerificationKey(), players, messages.get(sk));

            // Send the first set of messages.
            for (int from: test.sendBefore) {
                messages.get(new MockSigningKey(from)).make().prepare(Phase.BroadcastOutput,
                        new MockVerificationKey(test.me)).send();
            }

            {
                // Receive a message from an earlier phase to make sure we
                // flip through the first set of messages.
                messages.get(new MockSigningKey(1)).make().prepare(Phase.Shuffling,
                        new MockVerificationKey(test.me)).send();
            }

            try {
                mailbox.receiveFrom(new MockVerificationKey(1), Phase.Shuffling);
            } catch (WaitingException e) {
                Assert.fail();
            }

            // Then send the second set of messages.
            for (int from: test.sendAfter) {
                messages.get(new MockSigningKey(from)).make().prepare(Phase.BroadcastOutput,
                        new MockVerificationKey(test.me)).send();
            }

            Set<VerificationKey> receiveFrom = new HashSet<>();
            for (int from : test.receiveFrom) {
                receiveFrom.add(new MockVerificationKey(from));
            }

            // Now receive them all.
            Map<VerificationKey, com.shuffle.protocol.message.Message> received = null;
            try {
                received = mailbox.receiveFromMultiple(receiveFrom, Phase.BroadcastOutput);
                if (test.timeoutExpected) {
                    Assert.fail("Failed to throw WaitingException in test case " + i);
                }
            } catch (WaitingException e) {
                if (!test.timeoutExpected) {
                    Assert.fail();
                }
                continue;
            }

            // There should be one message for each participant.
            for (int from : test.receiveFrom) {
                if (!messages.containsKey(new MockSigningKey(from))) {
                    Assert.fail();
                }
                received.remove(new MockVerificationKey(from));
            }

            Assert.assertEquals(0, received.size());

            i++;
        }
    }
}
