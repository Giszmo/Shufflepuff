package com.shuffle.sim;

import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.SignedPacket;
import com.shuffle.protocol.TimeoutError;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This implementation of Network connects each shuffle machine to the simulator.
 *
 * Created by Daniel Krawisz on 2/8/16.
 */
class Network implements com.shuffle.protocol.Network {
    private Simulator simulator;
    final BlockingQueue<SignedPacket> inbox = new LinkedBlockingQueue<>();

    Network(Simulator simulator) {
        this.simulator = simulator;
    }

    @Override
    public void sendTo(VerificationKey to, SignedPacket packet) throws InvalidImplementationError, TimeoutError {

        try {
            simulator.sendTo(to, packet);
        } catch (InterruptedException e) {
            // This means that the thread running the machine we are delivering to has been interrupted.
            // This would look like a timeout if this were happening over a real network.
            throw new TimeoutError();
        }
    }

    @Override
    public SignedPacket receive() throws TimeoutError, InterruptedException {
        for (int i = 0; i < 2; i++) {
            SignedPacket next = inbox.poll(1, TimeUnit.SECONDS);

            if (next != null) {
                return next;
            }
        }

        throw new TimeoutError();
    }

    public void deliver(SignedPacket packet) throws InterruptedException {
        inbox.put(packet);
    }
}
