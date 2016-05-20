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
import com.shuffle.chan.BasicChan;
import com.shuffle.chan.Send;
import com.shuffle.chan.SigningSend;
import com.shuffle.chan.VerifyingSend;
import com.shuffle.chan.Inbox;
import com.shuffle.mock.MockSigningKey;
import com.shuffle.mock.MockVerificationKey;
import com.shuffle.p2p.Bytestring;
import com.shuffle.player.Messages;
import com.shuffle.protocol.message.Packet;
import com.shuffle.player.SessionIdentifier;
import com.shuffle.sim.MockMarshaller;

import org.junit.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Mock implementation of the Network interface for testing purposes.
 *
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MockNetwork  {
    public static class InnerMarshaller implements SigningSend.Marshaller<Packet> {

        @Override
        public Bytestring marshall(Packet packet) {

            ByteArrayOutputStream b = new ByteArrayOutputStream();
            try {
                ObjectOutputStream o = new ObjectOutputStream(b);
                o.writeObject(packet);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            return new Bytestring(b.toByteArray());
        }

        @Override
        public Messages.SignedPacket unmarshall(Bytestring string) {
            ByteArrayInputStream b = new ByteArrayInputStream(string.bytes);
            Object obj = null;
            try {
                ObjectInputStream o = new ObjectInputStream(b);
                obj = o.readObject();
            } catch (ClassNotFoundException | IOException e) {
                return null;
            }

            if (!(obj instanceof Messages.SignedPacket)) {
                return null;
            }

            return (Messages.SignedPacket)obj;
        }
    }

    // Haha yes it's silly to call an Inbox outbox. Normally you wouldn't use an Inbox
    // this way but for testing purposes it works fine because I just need to collect all
    // outgoing messages.
    private final Inbox<VerificationKey, com.shuffle.player.Packet> outbox;

    private final Map<VerificationKey, Send<Packet>> in = new HashMap<>();
    private final Map<VerificationKey, Send<com.shuffle.player.Packet>> out = new HashMap<>();

    private final Map<VerificationKey, Messages> messages = new HashMap<>();

    public MockNetwork(SessionIdentifier session, int me, int[] others, int cap) {
        // First create the inbox and outbox.
        outbox = new Inbox<>(cap);

        Inbox<VerificationKey, VerifyingSend.Signed<com.shuffle.player.Packet>> inbox
                = new Inbox<>(cap);

        VerificationKey vkme = new MockVerificationKey(me);

        InnerMarshaller om = new InnerMarshaller();
        MockMarshaller im = new MockMarshaller();

        for (int peer : others) {
            if (peer == me) continue;

            VerificationKey vkp = new MockVerificationKey(peer);
            SigningKey skp = new MockSigningKey(peer);

            Send<VerifyingSend.Signed<com.shuffle.player.Packet>> incoming = inbox.receivesFrom(vkp);
            Assert.assertTrue(incoming != null);

            // create a channel into the inbox.
            this.in.put(vkp,
                    new SigningSend<Packet>(
                            new VerifyingSend<>(incoming, im, vkp), om, skp));

            out.put(vkp, outbox.receivesFrom(vkp));

            messages.put(vkp, new Messages(session, vkp,
                    new HashMap<VerificationKey, Send<com.shuffle.player.Packet>>(),
                    new BasicChan<Inbox.Envelope<VerificationKey, VerifyingSend.Signed<com.shuffle.player.Packet>>>()));
        }

        messages.put(vkme, new Messages(session, vkme, out, inbox));
    }

    public Messages messages(VerificationKey k) {
        return messages.get(k);
    }

    public Inbox.Envelope<VerificationKey, com.shuffle.player.Packet> receive() throws InterruptedException {
        return outbox.receive();
    }

    // Send a message that is received by the test mailbox as if it was from k.
    public boolean sendFrom(VerificationKey k, com.shuffle.protocol.message.Packet p) throws InterruptedException {
        return in.get(k).send(p);
    }

    // This means we want to end the simulation and drain all messages sent by the
    // test mailbox.
    public List<Inbox.Envelope<VerificationKey, com.shuffle.player.Packet>> getResponses() throws InterruptedException {
        // First close all channels.
        for (Send<Packet> p : in.values()) {
            p.close();
        }

        for (Send<com.shuffle.player.Packet> p : out.values()) {
            p.close();
        }

        // Then close the outbox.
        outbox.close();

        // Then drain messages from outbox.
        List<Inbox.Envelope<VerificationKey, com.shuffle.player.Packet>> r = new LinkedList<>();

        while (true) {
            Inbox.Envelope<VerificationKey, com.shuffle.player.Packet> x = outbox.receive();

            if (x == null) return r;

            r.add(x);
        }
    }
}
