/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.player;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.Receive;
import com.shuffle.chan.Send;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Mock implementation of the Network interface for testing purposes.
 *
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MockNetwork implements Map<VerificationKey, Send<Packet>>, Receive<Messages.SignedPacket> {

    public class SentMessage implements Map.Entry<Packet, VerificationKey> {
        private final Packet packet;
        private VerificationKey to;

        public SentMessage(Packet packet, VerificationKey to) {
            this.to = to;
            this.packet = packet;
        }

        @Override
        public Packet getKey() {
            return packet;
        }

        @Override
        public VerificationKey getValue() {
            return to;
        }

        @Override
        public VerificationKey setValue(VerificationKey to) {
            return this.to = to;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }

            if (!(o instanceof MockNetwork.SentMessage)) {
                return false;
            }

            MockNetwork.SentMessage msg = (MockNetwork.SentMessage)o;
            return to.equals(msg.to) && packet.equals(msg.packet);
        }

        @Override
        public int hashCode() {
            return to.hashCode() + packet.hashCode();
        }

        @Override public String toString() {
            return packet.toString() + " -> " + to.toString();
        }
    }

    final public SigningKey me;

    final Queue<Map.Entry<Packet, VerificationKey>> responses;
    final Queue<Packet> received;

    public MockNetwork(SigningKey me) {
        this.me = me;
        this.received = new LinkedList<>();
        this.responses = new LinkedList<>();
    }

    @Override
    public int size() {
        return -1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsKey(Object o) {
        return (o instanceof VerificationKey);
    }

    @Override
    public boolean containsValue(Object o) {
        return false;
    }

    @Override
    public Send<Packet> get(final Object o) {
        if (!(o instanceof VerificationKey)) return null;

        if (me.VerificationKey().equals((VerificationKey)o)) {
            return new Send<Packet>() {

                @Override
                public boolean send(Packet packet) throws InterruptedException {
                    try {
                        return received.add(packet);
                    } finally {
                        System.out.println("      MockNetwork: receiving packet " + packet + "; queue is now " + received);
                    }
                }

                @Override
                public void close() {
                    // DO nothing.
                }
            };
        } else {

            return new Send<Packet>() {

                @Override
                public boolean send(Packet packet) throws InterruptedException {
                    System.out.println("      MockNetwork: sending response " + packet);
                    return responses.add(new SentMessage(packet, (VerificationKey)o));
                }

                @Override
                public void close() {
                    // Do nothing.
                }
            };
        }
    }

    @Override
    public Send<Packet> put(VerificationKey verificationKey, Send<Packet> packetSend) {
        return null;
    }

    @Override
    public Send<Packet> remove(Object o) {
        return null;
    }

    @Override
    public void putAll(Map<? extends VerificationKey, ? extends Send<Packet>> map) {

    }

    @Override
    public void clear() {

    }

    @Override
    public Set<VerificationKey> keySet() {
        return new HashSet<>();
    }

    @Override
    public Collection<Send<Packet>> values() {
        return new LinkedList<>();
    }

    @Override
    public Set<Entry<VerificationKey, Send<Packet>>> entrySet() {
        return new HashSet<>();
    }

    public Queue<Map.Entry<Packet, VerificationKey>> getResponses() {
        return responses;
    }

    @Override
    public Messages.SignedPacket receive() throws InterruptedException {
        Packet p = null;
        try {
            p = received.poll();
            if (p == null) return null;
            return new Messages.SignedPacket(p, me.makeSignature(p));
        } finally {
            System.out.println("      MockNetwork: reading response " + p);
        }
    }

    @Override
    public Messages.SignedPacket receive(long l, TimeUnit u) throws InterruptedException {
        Packet p = received.poll();
        if (p == null) return null;
        return new Messages.SignedPacket(p, me.makeSignature(p));
    }

    @Override
    public boolean closed() {
        return false;
    }
}
