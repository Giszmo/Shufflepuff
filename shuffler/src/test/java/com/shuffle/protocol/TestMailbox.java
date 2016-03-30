/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.mock.MockMessage;
import com.shuffle.mock.MockMessageFactory;
import com.shuffle.mock.MockNetwork;
import com.shuffle.mock.MockSessionIdentifier;
import com.shuffle.mock.MockSigningKey;
import com.shuffle.mock.MockVerificationKey;
import com.shuffle.protocol.blame.BlameException;

import org.junit.Assert;
import org.junit.Test;

import java.net.ProtocolException;
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
    public void testBroadcast() throws InvalidParticipantSetException {
        BroadcastTestCase tests[] =
                new BroadcastTestCase[]{
                        new BroadcastTestCase(1, 1),
                        new BroadcastTestCase(2, 1),
                        new BroadcastTestCase(2, 2),
                        new BroadcastTestCase(3, 2),
                        new BroadcastTestCase(4, 2),
                };

        int index = 0;
        for (BroadcastTestCase test : tests) {

            // Create mock network object.
            MockNetwork network = new MockNetwork();

            // make the set of players.
            Set<VerificationKey> players = new HashSet<>();
            for (int i = 1; i <= test.recipients; i ++) {
                players.add(new MockVerificationKey(i));
            }

            MockMessageFactory messages = new MockMessageFactory();

            MockSigningKey me = new MockSigningKey(test.sender);

            new Mailbox(new MockSessionIdentifier("testBroadcast" + index), me, players, network).broadcast(messages.make(), Phase.Shuffling);

            for (Map.Entry<SignedPacket, VerificationKey> sent : network.getResponses()) {
                VerificationKey sentBy = sent.getValue();
                Assert.assertTrue(players.contains(sentBy));
                players.remove(sentBy);
            }

            Assert.assertEquals(1, players.size());

            players.remove(me.VerificationKey());
            Assert.assertEquals(0, players.size());

            index++;
        }
    }

    static class sendToTestCase {
        public final int sender;
        public final int recipient;
        public final int players;
        public final boolean success;

        public sendToTestCase(int sender, int recipient, int players, boolean success) {
            this.sender = sender;
            this.recipient = recipient;
            this.players = players;
            this.success = success;
        }
    }

    @Test
    public void testSend() throws InvalidParticipantSetException {
        sendToTestCase tests[] = new sendToTestCase[]{
                // Case where recipient does not exist.
                new sendToTestCase(1, 3, 2, false),
                // Cannot send to myself.
                new sendToTestCase(1, 1, 2, false),
                // Success case.
                new sendToTestCase(1, 2, 2, true)
        };

        int index = 0;
        for (sendToTestCase test : tests) {
            // The player sending and inbox.
            MockSigningKey sk = new MockSigningKey(1);

            // Create mock network object.
            MockNetwork network = new MockNetwork();

            // make the set of players.
            Set<VerificationKey> players = new HashSet<>();
            for (int j = 1; j <= test.players; j ++) {
                players.add(new MockVerificationKey(j));
            }

            // Set up the shuffle machine (only used to query for the current phase).
            MockSessionIdentifier mockSessionIdentifier = new MockSessionIdentifier("testSend" + index);

            new Mailbox(
                    mockSessionIdentifier, sk, players, network
            ).send(new Packet(
                    new MockMessage(), mockSessionIdentifier,
                    Phase.Shuffling,
                    sk.VerificationKey(),
                    new MockVerificationKey(test.recipient)));

            int expected = (test.success ? 1 : 0);

            Queue<Map.Entry<SignedPacket, VerificationKey>> responses = network.getResponses();
            Assert.assertEquals(
                    String.format(
                            "Recieved %d responses when only expected %d in test case %d",
                            responses.size(), expected, index),
                    (test.success ? 1 : 0),
                    responses.size());

            for (Map.Entry msg : responses) {
                Assert.assertTrue("Received response does not equal expected", new MockVerificationKey(test.recipient).equals(msg.getValue()));
            }
            index++;
        }
    }

    static class receiveFromTestCase {
        public final int[] players;
        public final int requested; // The player that the message was expected from.
        public final Phase phase; // The expected phase.
        public final Packet packet;
        public final Error e; // If an exception is expected.

        public receiveFromTestCase(int[] players, int requested, Phase phase,Packet packet, Error e) {
            this.players = players;
            this.requested = requested;
            this.packet = packet;
            this.e = e;
            this.phase = phase;
        }
    }

    @Test(expected = TimeoutError.class)
    public void testReceiveFrom() throws InvalidParticipantSetException, InterruptedException, BlameException, ValueException, FormatException, SignatureException {
        receiveFromTestCase tests[] = new receiveFromTestCase[]{
                // time out exception test case.
                new receiveFromTestCase(new int []{1,2,3}, 2, Phase.Shuffling, null, new TimeoutError()),
                // Various malformed inputs.
                // TODO
        };

        int index = 0;
        for (receiveFromTestCase test : tests) {
            // The player sending and inbox.
            MockSigningKey sk = new MockSigningKey(1);

            // Create mock network object.
            MockNetwork network = new MockNetwork();

            // make the set of players.
            Set<VerificationKey> players = new HashSet<>();
            for (int j = 1; j <= test.players.length; j ++) {
                players.add(new MockVerificationKey(test.players[j - 1]));
            }

            MockSessionIdentifier mockSessionIdentifier = new MockSessionIdentifier("receiveFromTest" + index);

            new Mailbox(
                    mockSessionIdentifier, sk, players, network
            ).receiveFrom(new MockVerificationKey(test.requested), test.phase);
            index++;
        }
    }

    static class receiveFromMultipleTestCase {
        public final int players;
        public final int me;
        public final int[] receiveFrom; // Who are we expecting?
        public final int[] sendBefore; // Send before we call the function.
        public final int[] sendAfter; // Send after the function is called.
        public final boolean timeoutExpected;

        public receiveFromMultipleTestCase(int players, int me, int[] receiveFrom, int[] sendBefore, int[] sendAfter) {
            this.players = players;
            this.me = me;
            this.receiveFrom = receiveFrom;
            this.sendBefore = sendBefore;
            this.sendAfter = sendAfter;
            timeoutExpected = false;
        }

        public receiveFromMultipleTestCase(int players, int me, int[] receiveFrom, int[] sendBefore, int[] sendAfter, boolean timeoutExpected) {
            this.players = players;
            this.me = me;
            this.receiveFrom = receiveFrom;
            this.sendBefore = sendBefore;
            this.sendAfter = sendAfter;
            this.timeoutExpected = timeoutExpected;
        }
    }

    @Test
    public void testReceiveFromMultiple() throws InvalidParticipantSetException, InterruptedException, BlameException, ValueException, FormatException, ProtocolException, SignatureException {

        receiveFromMultipleTestCase tests[] = new receiveFromMultipleTestCase[]{
                // Very simple test case.
                new receiveFromMultipleTestCase(3, 2,
                        new int[]{3},
                        new int[]{},
                        new int[]{3}),
                // Timeout expected.
                new receiveFromMultipleTestCase(4, 2,
                        new int[]{3, 4},
                        new int[]{},
                        new int[]{3}, true),
                // before and after.
                new receiveFromMultipleTestCase(4, 2,
                        new int[]{3, 4},
                        new int[]{4},
                        new int[]{3})
        };

        int i = 0;
        for (receiveFromMultipleTestCase test : tests) {
            // The player sending and inbox.
            MockSigningKey sk = new MockSigningKey(test.me);

            // Create mock network object.
            MockNetwork network = new MockNetwork();

            // make the set of players.
            Set<VerificationKey> players = new HashSet<>();
            for (int j = 1; j <= test.players; j ++) {
                players.add(new MockVerificationKey(j));
            }

            MockSessionIdentifier mockSessionIdentifier = new MockSessionIdentifier("receiveFromMultiple" + i);

            // Set up the network operations object.
            Mailbox mailbox = new Mailbox(mockSessionIdentifier, sk, players, network);

            // Send the first set of messages.
            for (int from: test.sendBefore) {
                MockSigningKey sender = new MockSigningKey(from);

                network.deliver(
                        sender.makeSignedPacket(
                            new Packet(
                                new MockMessage(),
                                mockSessionIdentifier,
                                Phase.BroadcastOutput,
                                sender.VerificationKey(),
                                new MockVerificationKey(test.me))));
            }

            {
                // Receive a message from an earlier phase to make sure we flip through the first set of messages.
                MockSigningKey sender = new MockSigningKey(1);
                network.deliver(
                        sender.makeSignedPacket(
                                new Packet(
                                        new MockMessage(),
                                        mockSessionIdentifier,
                                        Phase.Shuffling,
                                        sender.VerificationKey(),
                                        new MockVerificationKey(test.me))));
            }

            try {
                mailbox.receiveFrom(new MockVerificationKey(1), Phase.Shuffling);
            } catch (TimeoutError e) {
                Assert.fail();
            }

            // Then send the second set of messages.
            for (int from: test.sendAfter) {
                MockSigningKey sender = new MockSigningKey(from);

                network.deliver(
                        sender.makeSignedPacket(
                                new Packet(
                                        new MockMessage(),
                                        mockSessionIdentifier,
                                        Phase.BroadcastOutput,
                                        sender.VerificationKey(),
                                        new MockVerificationKey(test.me))));
            }

            Set<VerificationKey> receiveFrom = new HashSet<>();
            for (int from : test.receiveFrom) {
                receiveFrom.add(new MockVerificationKey(from));
            }

            // Now receive them all.
            Map<VerificationKey, Message> messages = null;
            try {
                messages = mailbox.receiveFromMultiple(receiveFrom, Phase.BroadcastOutput);
                if (test.timeoutExpected) {
                    Assert.fail("Failed to throw TimeoutError in test case " + i);
                }
            } catch (TimeoutError e) {
                if (!test.timeoutExpected) {
                    Assert.fail();
                }
                continue;
            }

            // There should be one message for each participant.
            for (int from : test.receiveFrom) {
                if (!messages.containsKey(new MockVerificationKey(from))) {
                    Assert.fail();
                }
                messages.remove(new MockVerificationKey(from));
            }

            Assert.assertEquals(0, messages.size());

            i++;
        }
    }
}
