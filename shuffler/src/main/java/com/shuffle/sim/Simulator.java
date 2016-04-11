/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.sim;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.BasicChan;
import com.shuffle.chan.Chan;
import com.shuffle.player.MessageFactory;
import com.shuffle.monad.Either;
import com.shuffle.monad.NaturalSummableFuture;
import com.shuffle.monad.SummableFuture;
import com.shuffle.monad.SummableFutureZero;
import com.shuffle.monad.SummableMaps;
import com.shuffle.player.SessionIdentifier;
import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.Network;
import com.shuffle.protocol.blame.Matrix;
import com.shuffle.protocol.message.Packet;

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
        final Chan<Packet> inbox;
        final Map<VerificationKey, NetworkSim> networks;

        NetworkSim(Map<VerificationKey, NetworkSim> networks) {
            if (networks == null) {
                throw new NullPointerException();
            }
            this.networks = networks;

            inbox = new BasicChan<>(2 * (1 + networks.size()));
        }

        @Override
        public void sendTo(
                VerificationKey to,
                Packet packet
        ) throws InvalidImplementationError, InterruptedException {

            networks.get(to).deliver(packet);
        }

        @Override
        public Packet receive() throws InterruptedException {
            return inbox.receive(1000, TimeUnit.MILLISECONDS);
        }

        public void deliver(Packet packet) throws InterruptedException {
            inbox.send(packet);
        }
    }

    private Simulator() {
    }

    private static class SimulationInitializer implements InitialState.Initializer {
        public final Map<VerificationKey, NetworkSim> networks = new HashMap<>();
        public final SessionIdentifier session;

        private SimulationInitializer(SessionIdentifier session) {
            this.session = session;
        }

        @Override
        public com.shuffle.protocol.message.MessageFactory messages(VerificationKey key) {
            return new MessageFactory(session, key, network(key));
        }

        @Override
        public Network network(VerificationKey key) {
            NetworkSim network = new NetworkSim(networks);
            networks.put(key, network);
            return network;
        }
    }

    public static Map<SigningKey, Either<Transaction, Matrix>> run(InitialState init) {

        final SimulationInitializer initializer = new SimulationInitializer(init.session);
        final Map<SigningKey, Adversary> machines = init.getPlayers(initializer);

        Map<SigningKey, Either<Transaction, Matrix>> results = runSimulation(machines);

        initializer.networks.clear(); // Avoid memory leak.
        return results;
    }

    private static synchronized Map<SigningKey, Either<Transaction, Matrix>> runSimulation(
            Map<SigningKey, Adversary> machines)  {

        // Create a future for the set of entries.
        SummableFuture<Map<SigningKey, Either<Transaction, Matrix>>> wait
                = new SummableFutureZero<>(
                        new SummableMaps<SigningKey, Either<Transaction, Matrix>>()
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
