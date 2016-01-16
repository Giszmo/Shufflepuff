package com.shuffle.protocol;

import com.shuffle.bitcoin.VerificationKey;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MockNetwork implements Network {
    public static class SentMessage implements Map.Entry<Packet, MockVerificationKey> {
        Packet packet;
        MockVerificationKey to;

        public SentMessage(Packet packet, MockVerificationKey to) {
            this.to = to;
            this.packet = packet;
        }

        @Override
        public Packet getKey() {
            return packet;
        }

        @Override
        public MockVerificationKey getValue() {
            return to;
        }

        @Override
        public MockVerificationKey setValue(MockVerificationKey to) {
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

    Queue<Map.Entry<Packet, MockVerificationKey>> responses;
    Queue<Packet> sent;

    MockNetwork() {
        this.sent = new LinkedList<>();
        this.responses = new LinkedList<>();
    }

    MockNetwork(Queue<Packet> sent) {
        this.sent = sent;
        this.responses = new LinkedList<>();
    }

    Queue<Map.Entry<Packet, MockVerificationKey>> getResponses() {
        return responses;
    }

    @Override
    public void sendTo(VerificationKey to, Packet packet) throws InvalidImplementationError {
        if (!(to instanceof MockVerificationKey)) {
            throw new InvalidImplementationError();
        }

        final MockVerificationKey mockTo = (MockVerificationKey)to;

        responses.add(new SentMessage(packet, mockTo));
    }

    @Override
    public Packet receive() throws TimeoutError {
        if (sent.size() == 0) {
            throw new TimeoutError();
        }

        return sent.remove();
    }

}
