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
import com.shuffle.chan.BasicInbox;
import com.shuffle.chan.Send;
import com.shuffle.chan.packet.JavaMarshaller;
import com.shuffle.chan.packet.Packet;
import com.shuffle.chan.packet.Signed;
import com.shuffle.chan.Inbox;
import com.shuffle.player.Messages;
import com.shuffle.player.P;
import com.shuffle.chan.packet.SessionIdentifier;

import org.junit.Assert;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mock implementation of the Network interface for testing purposes.
 *
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MockNetwork  {

    // Haha yes it's silly to call an Inbox outbox. Normally you wouldn't use an Inbox
    // this way but for testing purposes it works fine because I just need to collect all
    // outgoing messages.
    private final Inbox<VerificationKey, Signed<Packet<VerificationKey, P>>> outbox;

    private final Map<VerificationKey, Send<Signed<Packet<VerificationKey, P>>>> out = new HashMap<>();

    private final Map<VerificationKey, Messages> messages = new HashMap<>();

    public MockNetwork(SessionIdentifier session, SigningKey me, Set<SigningKey> others, int cap) {
        // First create the inbox and outbox.
        outbox = new BasicInbox<>(cap);

        Inbox<VerificationKey, Signed<Packet<VerificationKey, P>>> inbox
                = new BasicInbox<>(cap);

        VerificationKey vk = me.VerificationKey();

        JavaMarshaller<Packet<VerificationKey, P>> jm = new JavaMarshaller<>();

        for (SigningKey skp : others) {
            if (skp.equals(me)) continue;

            VerificationKey vkp = skp.VerificationKey();

            // Create a spot in the outbox for messages sent to this peer.
            out.put(vkp, outbox.receivesFrom(vkp));

            Send<Signed<Packet<VerificationKey, P>>> incoming = inbox.receivesFrom(vkp);
            Assert.assertTrue(incoming != null);

            HashMap<VerificationKey, Send<Signed<Packet<VerificationKey, P>>>> outFrom = new HashMap<>();
            outFrom.put(vk, incoming);

            messages.put(vkp, new Messages(session, skp, outFrom,
                    new BasicChan<Inbox.Envelope<VerificationKey, Signed<Packet<VerificationKey, P>>>>()));

        }

        messages.put(vk, new Messages(session, me, out, inbox));
    }

    public Messages messages(VerificationKey k) {
        return messages.get(k);
    }

    // This means we want to end the simulation and drain all messages sent by the
    // test mailbox.
    public List<Inbox.Envelope<VerificationKey, Signed<Packet<VerificationKey, P>>>> getResponses() throws InterruptedException {
        // First close all channels.
        for (Send<Signed<Packet<VerificationKey, P>>> p : out.values()) {
            p.close();
        }

        // Then close the outbox.
        outbox.close();

        // Then drain messages from outbox.
        List<Inbox.Envelope<VerificationKey, Signed<Packet<VerificationKey, P>>>> r = new LinkedList<>();

        while (true) {
            Inbox.Envelope<VerificationKey, Signed<Packet<VerificationKey, P>>> x = outbox.receive();

            if (x == null) return r;

            r.add(x);
        }
    }
}