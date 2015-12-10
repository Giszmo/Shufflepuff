package com.shuffle.form;

import java.util.Map;
import java.util.Queue;

/**
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MockNetwork implements Network {
    Queue<Map.Entry<MockVerificationKey, MockPacket>> responses;
    Queue<MockPacket> sent;

    @Override
    public void sendTo(VerificationKey to, Packet packet) throws InvalidImplementationException {
        if (!(packet instanceof MockPacket)) {
            throw new InvalidImplementationException();
        }

        if (!(to instanceof MockVerificationKey)) {
            throw new InvalidImplementationException();
        }

        final MockVerificationKey mockTo = (MockVerificationKey)to;
        final MockPacket mockPacket = (MockPacket)packet;

        responses.add(new Map.Entry<MockVerificationKey, MockPacket>() {
            MockVerificationKey to = mockTo;
            MockPacket packet = mockPacket;

            @Override
            public MockVerificationKey getKey() {
                return to;
            }

            @Override
            public MockPacket getValue() {
                return packet;
            }

            @Override
            public MockPacket setValue(MockPacket mockPacket) {
                return packet = mockPacket;
            }

            @Override
            public boolean equals(Object o) {
                return false; // Probably not needed.
            }

            @Override
            public int hashCode() {
                return mockTo.hashCode() ^ mockPacket.hashCode();
            }
        });
    }

    @Override
    public Packet receive() throws TimeoutException {
        if (sent.size() == 0) {
            throw new TimeoutException();
        }

        return sent.remove();
    }

}
