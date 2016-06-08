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
import com.shuffle.chan.Inbox;

import com.shuffle.chan.packet.Packet;
import com.shuffle.chan.packet.Signed;
import com.shuffle.player.Messages;
import com.shuffle.mock.MockSessionIdentifier;
import com.shuffle.mock.MockSigningKey;
import com.shuffle.mock.MockVerificationKey;
import com.shuffle.chan.packet.SessionIdentifier;
import com.shuffle.player.P;
import com.shuffle.protocol.blame.BlameException;
import com.shuffle.protocol.message.Phase;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Tests for certain functions pertaining to network interactions.
 *
 * Created by Daniel Krawisz on 12/7/15.
 */
public class TestMailbox {

    static class BroadcastTestCase {
        public final SortedSet<SigningKey> recipients = new TreeSet<>();
        public final int sender;

        public BroadcastTestCase(int recipients, int sender) {

            for (int i = 0; i < recipients; i++) {
                this.recipients.add(new MockSigningKey(i + 1));
            }
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

        int testCase = 0;
        for (BroadcastTestCase test : tests) {
            testCase++;
            System.out.println("Test case " + testCase);

            // make the set of players.
            Set<VerificationKey> players = new HashSet<>();
            for (SigningKey k : test.recipients) {
                players.add(k.VerificationKey());
            }

            MockSigningKey me = new MockSigningKey(test.sender);
            SessionIdentifier session = new MockSessionIdentifier("test broadcast");

            MockNetwork network = new MockNetwork(session, me, test.recipients, 100);

            Messages messages = network.messages(me.VerificationKey());

            new Mailbox(me.VerificationKey(), players, messages
            ).broadcast(messages.make(), Phase.Shuffling);

            for (Inbox.Envelope<VerificationKey, Signed<Packet<VerificationKey, P>>> sent
                    : network.getResponses()) {

                VerificationKey sentTo = sent.from;
                Assert.assertTrue(players.contains(sentTo));
                players.remove(sentTo);
            }

            Assert.assertTrue(players.contains(me.VerificationKey()));
        }
    }

    static class SendToTestCase {
        public final int sender;
        public final int recipient;
        public final SortedSet<SigningKey> players = new TreeSet<>();
        public final boolean success;

        public SendToTestCase(int sender, int recipient, int players, boolean success) {
            this.sender = sender;
            this.recipient = recipient;
            this.success = success;

            for (int i = 0; i < players; i++) {
                this.players.add(new MockSigningKey(i + 1));
            }
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
            MockSigningKey sk = new MockSigningKey(test.sender);

            // Create mock network object.
            SessionIdentifier session = new MockSessionIdentifier("testSend" + index);
            MockNetwork network = new MockNetwork(session, sk, test.players, 100);

            // make the set of players.
            Set<VerificationKey> players = new HashSet<>();
            for (SigningKey j : test.players) {
                players.add(j.VerificationKey());
            }

            // Set up the shuffle machine (only used to query for the current phase).
            Messages messages = network.messages(sk.VerificationKey());

            new Mailbox(
                    sk.VerificationKey(), players, messages
            ).send(messages.make(), Phase.Shuffling, new MockVerificationKey(test.recipient));

            int expected = (test.success ? 1 : 0);

            List<Inbox.Envelope<VerificationKey, Signed<Packet<VerificationKey, P>>>> responses = network.getResponses();
            Assert.assertEquals(
                    String.format(
                            "Recieved %d responses when only expected %d in test case %d",
                            responses.size(), expected, index),
                    (test.success ? 1 : 0),
                    responses.size());

            for (Inbox.Envelope<VerificationKey, Signed<Packet<VerificationKey, P>>> msg : responses) {
                Assert.assertTrue("Received response does not equal expected",
                        new MockVerificationKey(test.recipient).equals(msg.from));
            }
            index++;
        }
    }

    static class ReceiveFromTestCase {
        public final SortedSet<SigningKey> players = new TreeSet<>();
        public final int recipient;
        public final int requested; // The player that the message was expected from.
        public final Phase phase; // The expected phase.
        public final Packet packet;
        public final boolean error; // If an exception is expected.

        public ReceiveFromTestCase(
                int[] players,
                int recipient,
                int requested,
                Phase phase,
                Packet packet,
                boolean error
        ) {
            this.recipient = recipient;
            this.requested = requested;
            this.packet = packet;
            this.error = error;
            this.phase = phase;

            for (int player : players) {
                this.players.add(new MockSigningKey(player));
            }
        }
    }

    @Test()
    public void testReceiveFrom()
            throws InvalidParticipantSetException, InterruptedException,
            BlameException, FormatException, WaitingException, IOException {

        ReceiveFromTestCase[] tests = new ReceiveFromTestCase[]{
                // time out exception test case.
                new ReceiveFromTestCase(
                        new int []{1,2,3}, 1, 2, Phase.Shuffling, null, true
                )
                // Various malformed inputs.
                // TODO
        };

        int index = 0;
        for (ReceiveFromTestCase test : tests) {
            // The player sending and inbox.
            MockSigningKey sk = new MockSigningKey(test.recipient);

            // make the set of players.
            Set<VerificationKey> players = new HashSet<>();
            for (SigningKey j : test.players) {
                players.add(j.VerificationKey());
            }

            SessionIdentifier session = new MockSessionIdentifier("receiveFromTest" + index);
            MockNetwork network = new MockNetwork(session, sk, test.players, 100);

            try {
                new Mailbox(sk.VerificationKey(), players, network.messages(sk.VerificationKey())
                ).receiveFrom(new MockVerificationKey(test.requested), test.phase);
            } catch (WaitingException e) {
                if (test.packet != null) {
                    Assert.fail("Waiting exception caught when not expected.");
                }
            }
            index++;
        }
    }

    static class ReceiveFromMultipleTestCase {
        public final SortedSet<SigningKey> players = new TreeSet<>();
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

            this.me = me;
            this.receiveFrom = receiveFrom;
            this.sendBefore = sendBefore;
            this.sendAfter = sendAfter;
            timeoutExpected = false;

            for (int i = 0; i < players; i++) {
                this.players.add(new MockSigningKey(i + 1));
            }
        }

        public ReceiveFromMultipleTestCase(
                int players,
                int me,
                int[] receiveFrom,
                int[] sendBefore,
                int[] sendAfter,
                boolean timeoutExpected
        ) {

            this.me = me;
            this.receiveFrom = receiveFrom;
            this.sendBefore = sendBefore;
            this.sendAfter = sendAfter;
            this.timeoutExpected = timeoutExpected;

            for (int i = 0; i < players; i++) {
                this.players.add(new MockSigningKey(i + 1));
            }

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
            System.out.println("receive from multiple test case " + i + "; me = " + test.me);

            // The player sending and inbox.
            SigningKey sk = new MockSigningKey(test.me);
            VerificationKey vk = sk.VerificationKey();

            SessionIdentifier session = new MockSessionIdentifier("receiveFromMultiple" + i);

            // Create mock network object.
            MockNetwork network = new MockNetwork(session, sk, test.players, 100);

            // make the set of players.
            Set<VerificationKey> players = new HashSet<>();
            for (SigningKey j : test.players) {
                if (j.equals(sk)) continue;

                players.add(j.VerificationKey());
            }

            // Set up the network operations object.
            Mailbox mailbox = new Mailbox(sk.VerificationKey(), players, network.messages(vk));

            // Send the first set of messages.
            for (int from : test.sendBefore) {
                VerificationKey k = new MockVerificationKey(from);
                network.messages(k).make().send(Phase.BroadcastOutput, vk);

            }

            VerificationKey disorderedSender = new MockVerificationKey((test.me % test.players.size()) + 1);
            // Receive a message from an earlier phase to make sure we
            // flip through the first set of messages.
            Assert.assertNotNull(network.messages(disorderedSender).make().send(Phase.Shuffling, vk));

            try {
                mailbox.receiveFrom(disorderedSender, Phase.Shuffling);
            } catch (WaitingException e) {
                Assert.fail();
            }

            // Then send the second set of messages.
            for (int from: test.sendAfter) {
                VerificationKey k = new MockVerificationKey(from);
                network.messages(k).make().send(Phase.BroadcastOutput, vk);
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
                received.remove(new MockVerificationKey(from));
            }

            Assert.assertEquals(0, received.size());

            i++;
        }
    }
}