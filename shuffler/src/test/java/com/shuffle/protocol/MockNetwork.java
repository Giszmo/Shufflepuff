package com.shuffle.protocol;

import com.shuffle.bitcoin.VerificationKey;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Mock implementation of the Network interface for testing purposes.
 *
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MockNetwork implements Network {
    public static class SentMessage implements Map.Entry<SignedPacket, VerificationKey> {
        SignedPacket packet;
        VerificationKey to;

        public SentMessage(SignedPacket packet, VerificationKey to) {
            this.to = to;
            this.packet = packet;
        }

        @Override
        public SignedPacket getKey() {
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

        @Override public String toString(){
            return packet.toString() + " -> " + to.toString();
        }
    }

    Queue<Map.Entry<SignedPacket, VerificationKey>> responses;
    Queue<SignedPacket> sent;

    MockNetwork() {
        this.sent = new LinkedList<>();
        this.responses = new LinkedList<>();
    }

    MockNetwork(Queue<SignedPacket> sent) {
        this.sent = sent;
        this.responses = new LinkedList<>();
    }

    Queue<Map.Entry<SignedPacket, VerificationKey>> getResponses() {
        return responses;
    }

    @Override
    public void sendTo(VerificationKey to, SignedPacket packet) {

        responses.add(new SentMessage(packet, to));
    }

    @Override
    public SignedPacket receive() throws TimeoutError {
        if (sent.size() == 0) {
            throw new TimeoutError();
        }

        return sent.remove();
    }

    public void deliver(SignedPacket packet) {
        sent.add(packet);
    }
}
