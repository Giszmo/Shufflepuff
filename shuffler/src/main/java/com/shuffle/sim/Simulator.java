package com.shuffle.sim;

import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.monad.NaturalSummableFuture;
import com.shuffle.monad.SummableFuture;
import com.shuffle.monad.SummableFutureZero;
import com.shuffle.chan.Chan;
import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.Machine;
import com.shuffle.protocol.MessageFactory;
import com.shuffle.protocol.Network;
import com.shuffle.protocol.SignedPacket;
import com.shuffle.protocol.TimeoutError;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private static Logger log= LogManager.getLogger(Simulator.class);

    /**
     * A network connecting different players together in a simulation.
     *
     * Created by Daniel Krawisz on 2/8/16.
     */
    private class NetworkSim implements Network {
        final Chan<SignedPacket> inbox = new Chan<>();
        final Map<VerificationKey, NetworkSim> networks;

        NetworkSim(Map<VerificationKey, NetworkSim> networks) {
            if (networks == null) {
                throw new NullPointerException();
            }
            this.networks = networks;
        }

        @Override
        public void sendTo(VerificationKey to, SignedPacket packet) throws InvalidImplementationError, TimeoutError {

            try {
                networks.get(to).deliver(packet);
            } catch (InterruptedException e) {
                // This means that the thread running the machine we are delivering to has been interrupted.
                // This would look like a timeout if this were happening over a real network.
                throw new TimeoutError();
            }
        }

        @Override
        public SignedPacket receive() throws TimeoutError, InterruptedException {
            for (int i = 0; i < 2; i++) {
                SignedPacket next = inbox.receive(1, TimeUnit.SECONDS);

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

    final MessageFactory messages;

    public Simulator(MessageFactory messages)  {
        this.messages = messages;
    }

    public Map<SigningKey, Machine> run(InitialState init, Crypto crypto) {

        final Map<SigningKey, Adversary> machines = new HashMap<>();
        final Map<VerificationKey, NetworkSim> networks = new HashMap<>();

        // Check that all players have a coin network set up, either the default or their own.
        for (InitialState.PlayerInitialState player : init.getPlayers()) {
            if (player.sk == null) {
                continue;
            }

            NetworkSim network = new NetworkSim(networks);
            networks.put(player.vk, network);

            Adversary adversary = player.adversary(crypto, messages, network);
            machines.put(player.sk, adversary);
        }

        Map<SigningKey, Machine> results = runSimulation(machines);

        networks.clear(); // Avoid memory leak.
        return results;
    }

    private static synchronized Map<SigningKey, Machine> runSimulation(
            Map<SigningKey, Adversary> machines)  {

        // Create a future for the set of entries.
        SummableFuture<Map<SigningKey, Machine>> wait = new SummableFutureZero<Map<SigningKey, Machine>>();

        // Start the simulations.
        for (Adversary in : machines.values()) {
            wait = wait.plus(new NaturalSummableFuture<Map<SigningKey, Machine>>(in.turnOn()));
        }

        try {
            return wait.get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }
}
