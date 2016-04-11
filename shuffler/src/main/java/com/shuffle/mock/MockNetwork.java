/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.mock;

import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.Network;
import com.shuffle.protocol.message.Packet;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Mock implementation of the Network interface for testing purposes.
 *
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MockNetwork implements Network {
    public static class SentMessage implements Map.Entry<Packet, VerificationKey> {
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

            if (!(o instanceof SentMessage)) {
                return false;
            }

            SentMessage msg = (SentMessage)o;
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

    final Queue<Map.Entry<Packet, VerificationKey>> responses;
    final Queue<Packet> received;

    public MockNetwork() {
        this.received = new LinkedList<>();
        this.responses = new LinkedList<>();
    }

    MockNetwork(Queue<Packet> received) {
        this.received = received;
        this.responses = new LinkedList<>();
    }

    public Queue<Map.Entry<Packet, VerificationKey>> getResponses() {
        return responses;
    }

    @Override
    public void sendTo(VerificationKey to, Packet packet) {

        responses.add(new SentMessage(packet, to));
    }

    @Override
    public Packet receive() {
        if (received.size() == 0) {
            return null;
        }

        return received.remove();
    }

    public void deliver(Packet packet) {
        received.add(packet);
    }
}
