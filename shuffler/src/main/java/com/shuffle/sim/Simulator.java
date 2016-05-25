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
import com.shuffle.monad.Either;
import com.shuffle.monad.NaturalSummableFuture;
import com.shuffle.monad.SummableFuture;
import com.shuffle.monad.SummableFutureZero;
import com.shuffle.monad.SummableMaps;
import com.shuffle.protocol.blame.Matrix;
import com.shuffle.protocol.message.Packet;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * A simulator for running integration tests on the protocol.
 *
 * Created by Daniel Krawisz on 12/6/15.
 */
public final class Simulator {
    private static final Logger log = LogManager.getLogger(Simulator.class);

    // Cannot be instantiated. Everything here is static!
    private Simulator() {
    }

    public static Map<SigningKey, Either<Transaction, Matrix>> run(InitialState init) {

        final Initializer<Packet> initializer = new Initializer<>(init.session, new MockMarshaller(), 2 * (1 + init.size() ));
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
