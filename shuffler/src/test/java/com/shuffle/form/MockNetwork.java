package com.shuffle.form;

import java.util.Queue;

/**
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MockNetwork implements Network {
    Queue<Packet> responses;
    Queue<Packet> sent;


    @Override
    public void sendTo(VerificationKey to, Packet packet) throws InvalidImplementationException {
        if (!(packet instanceof MockPacket)) {
            throw new InvalidImplementationException();
        }

        if (!(to instanceof MockVerificationKey)) {
            throw new InvalidImplementationException();
        }

        ((MockPacket)packet).to = (MockVerificationKey)to;

        responses.add(packet);
    }

    @Override
    public Packet receive() throws TimeoutException {
        if (sent.size() == 0) {
            throw new TimeoutException();
        }

        return sent.remove();
    }

}
