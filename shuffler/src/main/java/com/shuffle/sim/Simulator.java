/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.sim;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.Chan;
import com.shuffle.mock.MockMessageFactory;
import com.shuffle.monad.NaturalSummableFuture;
import com.shuffle.monad.SummableFuture;
import com.shuffle.monad.SummableFutureZero;
import com.shuffle.monad.SummableMaps;
import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.Machine;
import com.shuffle.protocol.MessageFactory;
import com.shuffle.protocol.Network;
import com.shuffle.protocol.SignedPacket;
import com.shuffle.protocol.TimeoutError;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * A simulator for running integration tests on the protocol.
 *
 * Created by Daniel Krawisz on 12/6/15.
 */
public final class Simulator {
    private static final Logger log = LogManager.getLogger(Simulator.class);

    /**
     * A network connecting different players together in a simulation.
     *
     * Created by Daniel Krawisz on 2/8/16.
     */
    private static class NetworkSim implements Network {
        final Chan<SignedPacket> inbox;
        final Map<VerificationKey, NetworkSim> networks;

        NetworkSim(Map<VerificationKey, NetworkSim> networks) {
            if (networks == null) {
                throw new NullPointerException();
            }
            this.networks = networks;

            inbox = new Chan<>(2 * (1 + networks.size()));
        }

        @Override
        public void sendTo(
                VerificationKey to,
                SignedPacket packet
        ) throws InvalidImplementationError, TimeoutError {

            try {
                networks.get(to).deliver(packet);
            } catch (InterruptedException e) {
                // This means that the thread running the machine we are delivering to
                // has been interrupted. This would look like a timeout if this were
                // happening over a real network.
                throw new TimeoutError();
            }
        }

        @Override
        public SignedPacket receive() throws TimeoutError, InterruptedException {
            for (int i = 0; i < 2; i++) {
                SignedPacket next = inbox.receive(300, TimeUnit.MILLISECONDS);

                if (next != null) {
                    return next;
                }
            }

            throw new TimeoutError();
        }

        public void deliver(SignedPacket packet) throws InterruptedException {
            boolean sent = inbox.send(packet);
        }
    }

    private Simulator() {
    }

    private static class SimulationInitializer implements InitialState.Initializer {
        public final Map<VerificationKey, NetworkSim> networks = new HashMap<>();

        @Override
        public MessageFactory messages(VerificationKey key) {
            return new MockMessageFactory();
        }

        @Override
        public Network network(VerificationKey key) {
            NetworkSim network = new NetworkSim(networks);
            networks.put(key, network);
            return network;
        }
    }

    public static Map<SigningKey, Machine> run(InitialState init, MessageFactory messages) {

        final SimulationInitializer initializer = new SimulationInitializer();
        final Map<SigningKey, Adversary> machines = init.getPlayers(initializer);

        Map<SigningKey, Machine> results = runSimulation(machines);

        initializer.networks.clear(); // Avoid memory leak.
        return results;
    }

    private static synchronized Map<SigningKey, Machine> runSimulation(
            Map<SigningKey, Adversary> machines)  {

        // Create a future for the set of entries.
        SummableFuture<Map<SigningKey, Machine>> wait = new SummableFutureZero<>(
                new SummableMaps<SigningKey, Machine>()
        );

        // Start the simulations.
        for (Adversary in : machines.values()) {
            wait = wait.plus(new NaturalSummableFuture<>(in.turnOn()));
        }

        try {
            return wait.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Returning null. This indicates that some player returned an exception "
                    + "and was not able to complete the protocol.");
            return null;
        }
    }
}
