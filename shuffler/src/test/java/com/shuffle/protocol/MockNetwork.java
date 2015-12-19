package com.shuffle.protocol;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MockNetwork implements Network {
    public static class SentMessage implements Map.Entry<MockMessage, MockVerificationKey> {
        MockMessage packet;
        MockVerificationKey to;

        public SentMessage(MockMessage packet, MockVerificationKey to) {
            this.to = to;
            this.packet = packet;
        }

        @Override
        public MockMessage getKey() {
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

    Queue<Map.Entry<MockMessage, MockVerificationKey>> responses;
    Queue<MockMessage> sent;

    MockNetwork() {
        this.sent = new LinkedList<>();
        this.responses = new LinkedList<>();
    }

    MockNetwork(Queue<MockMessage> sent) {
        this.sent = sent;
        this.responses = new LinkedList<>();
    }

    Queue<Map.Entry<MockMessage, MockVerificationKey>> getResponses() {
        return responses;
    }

    @Override
    public void sendTo(VerificationKey to, Message message) throws InvalidImplementationException {
        if (!(message instanceof MockMessage)) {
            throw new InvalidImplementationException();
        }

        if (!(to instanceof MockVerificationKey)) {
            throw new InvalidImplementationException();
        }

        final MockVerificationKey mockTo = (MockVerificationKey)to;
        final MockMessage mockPacket = (MockMessage) message;

        responses.add(new SentMessage(mockPacket, mockTo));
    }

    @Override
    public Message receive() throws TimeoutException {
        if (sent.size() == 0) {
            throw new TimeoutException();
        }

        return sent.remove();
    }

}
