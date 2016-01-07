package com.shuffle.protocol;

import com.shuffle.cryptocoin.Coin;
import com.shuffle.cryptocoin.Crypto;
import com.shuffle.cryptocoin.CryptographyError;
import com.shuffle.cryptocoin.SigningKey;
import com.shuffle.cryptocoin.Transaction;
import com.shuffle.cryptocoin.VerificationKey;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A simulator for running integration tests on the protocol. Right now it only does positive tests,
 * but eventually it will also simulate malicious players.
 *
 * Created by Daniel Krawisz on 12/6/15.
 */
public final class Simulator {
    final ConcurrentMap<VerificationKey, BlackBox> machines;
    final SortedSet<VerificationKey> players;
    final SessionIdentifier τ;
    final long amount;
    final MessageFactory messages;
    final Crypto crypto;
    final Coin coin;

    // This implementation of Network connects each shuffle machine to the simulator.
    private class Network implements com.shuffle.protocol.Network {
        final BlockingQueue<Packet> inbox = new LinkedBlockingQueue<>();
        final Map<ShufflePhase, Map<VerificationKey, Packet>> malicious = new HashMap<>(); // Replacement messages to simulate malicious players.

        Network() {
        }

        Network(Map<ShufflePhase, Map<VerificationKey, Packet>> malicious) {
            malicious.putAll(malicious);
        }

        @Override
        public void sendTo(VerificationKey to, Packet packet) throws InvalidImplementationError, TimeoutError {
            // Replace with malicious packet if necessary.
            Map<VerificationKey, Packet> replacements = malicious.get(packet.phase);
            if (replacements != null) {
                Packet maliciousPacket = replacements.get(to);
                if (maliciousPacket != null) {
                    packet = maliciousPacket;
                }
            }

            try {
                Simulator.this.sendTo(to, new Packet(messages.copy(packet.message), packet.τ, packet.phase, packet.signer, packet.recipient));
            } catch (InterruptedException e) {
                // This means that the thread running the machine we are delivering to has been interrupted.
                // This would look like a timeout if this were happening over a real network.
                throw new TimeoutError();
            }
        }

        @Override
        public Packet receive() throws TimeoutError, InterruptedException {
            Packet next = inbox.poll(1, TimeUnit.SECONDS);
            if (next == null) {
                throw new TimeoutError();
            }
            return next;
        }

        public void deliver(Packet packet) throws InterruptedException {
            inbox.put(packet);
        }
    }

    // Simulations can have a pretty complicated initial state. This class is
    // for initializing a single machine in the simulation.
    public static class InitialState {
        public SigningKey key;

        public InitialState(SigningKey key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return "initial[" + key.toString() + "]";
        }
    }

    // Could be a real or a malicious player.
    private interface Adversary {
        ReturnState turnOn() throws InvalidImplementationError;
        ShufflePhase currentPhase();
        void deliver(Packet packet) throws InterruptedException;
        SigningKey identity();
    }

    // A wraper for the regular version of the protocol.
    private class HonestAdversary implements Adversary {
        CoinShuffle.ShuffleMachine machine;
        Network network;
        SigningKey sk;

        HonestAdversary(SessionIdentifier τ, long amount, SigningKey sk,
                        SortedSet<VerificationKey> players) {
            this.sk = sk;
            this.network = new Network();
            this.machine = new CoinShuffle(messages, crypto, coin, network).new ShuffleMachine(τ, amount, sk, players, null, 1, 2);
        }

        @Override
        public ReturnState turnOn() throws InvalidImplementationError {
            try {
                return machine.run();
            } catch (InterruptedException e) {
                return new ReturnState(false, τ, machine.currentPhase(), e, null);
            }
        }

        @Override
        public ShufflePhase currentPhase() {
            return machine.currentPhase();
        }

        @Override
        public void deliver(Packet packet) throws InterruptedException {
            network.deliver(packet);
        }

        @Override
        public SigningKey identity() {
            return sk;
        }
    }

    // The malicious adversary can be made to send malicious messages or to spend bitcoins at
    // inappropriate times.
    private class MaliciousAdversary implements Adversary{
        final CoinShuffle.ShuffleMachine machine;
        final Network network;
        final SigningKey sk;

        final Transaction t;
        boolean transactionSent = false;

        MaliciousAdversary(SessionIdentifier τ, long amount, SigningKey sk,
                        SortedSet<VerificationKey> players, final Map<ShufflePhase, Map<VerificationKey, Packet>> lies,
                        Transaction t) {
            this.sk = sk;
            if (lies == null) {
                this.network = new Network();
            } else {
                this.network = new Network(lies);
            }
            this.t = t;
            this.machine = new CoinShuffle(messages, crypto, coin, network).new ShuffleMachine(τ, amount, sk, players, null, 1, 2);
        }

        @Override
        public ReturnState turnOn() throws InvalidImplementationError {
            try {
                machine.run();
                // Malicious players don't need to return any information since they are not being tested.
                return new ReturnState(false, τ, ShufflePhase.Malicious, null, null);
            } catch (InterruptedException e) {
                return new ReturnState(false, τ, machine.currentPhase(), e, null);
            }
        }

        @Override
        public ShufflePhase currentPhase() {
            return machine.currentPhase();
        }

        @Override
        public void deliver(Packet packet) throws InterruptedException {
            // Part way through the protocol, send the malicious bitcoin transaction. 
            if (packet.phase == ShufflePhase.BroadcastOutput && !transactionSent) {
                coin.send(t);
                transactionSent = true;
            }
            network.deliver(packet);
        }

        @Override
        public SigningKey identity() {
            return sk;
        }
    }

    // A wrapper for an adversary that can run a player as a separate thread. Could be good or evil!
    private class BlackBox implements Runnable {
        Adversary machine;
        Thread thread;

        // Used to send a message from the new thread to the old one.
        BlockingQueue<ReturnState> q;

        public BlackBox(Adversary machine) {
            this.machine = machine;
            thread = null;
            q = new LinkedBlockingQueue<>();
        }

        public void deliver(Packet packet) throws InvalidImplementationError, InterruptedException {
            machine.deliver(packet);
        }

        public void interrupt() {
            if (thread != null) {
                thread.interrupt();
            }
        }

        public Future<Map.Entry<SigningKey, ReturnState>> doIt() {
            class entry implements Map.Entry<SigningKey, ReturnState> {
                SigningKey key;
                ReturnState re;

                public entry(SigningKey key, ReturnState re) {
                    this.key = key;
                    this.re = re;
                }

                @Override
                public SigningKey getKey() {
                    return key;
                }

                @Override
                public ReturnState getValue() {
                    return re;
                }

                @Override
                public ReturnState setValue(ReturnState returnState) {
                    re = returnState;
                    return re;
                }
            }

            // Start new thread.
            thread = new Thread(this);
            thread.start();

            // Wait for message from new thread.
            return new Future<Map.Entry<SigningKey, ReturnState>>() {
                @Override
                public boolean cancel(boolean b) {
                    return false;
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public boolean isDone() {
                    return !q.isEmpty();
                }

                @Override
                public Map.Entry<SigningKey, ReturnState> get() throws InterruptedException, ExecutionException {
                    return new entry(machine.identity(), q.take());
                }

                @Override
                public Map.Entry<SigningKey, ReturnState> get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {
                    return new entry(machine.identity(), q.poll(l, timeUnit));
                }
            };
        }

        @Override
        public void run() {
            try {
                q.add(machine.turnOn());
            } catch (InvalidImplementationError e) {
                q.add(new ReturnState(false, τ, machine.currentPhase(), e, null));
            }
        }
    }

    public Simulator(SessionIdentifier τ, long amount, List<InitialState> init, MessageFactory messages, Crypto crypto, Coin coin)  {
        if (τ == null || init == null) throw new NullPointerException();
        this.τ = τ;
        this.amount = amount;
        this.players = new TreeSet<>();
        this.messages = messages;
        this.crypto = crypto;
        this.coin = coin;

        machines = new ConcurrentHashMap<>();

        int i = 0;
        try {
            for (InitialState in : init) {
                players.add(in.key.VerificationKey());
                machines.put(in.key.VerificationKey(),
                        new BlackBox(new HonestAdversary(τ, amount, in.key, players)));
                i++;
            }
        } catch (CryptographyError e) {
            e.printStackTrace();
        }

    }

    public Map<SigningKey, ReturnState> runSimulation() {
        //Timer timer = new Timer();
        List<Future<Map.Entry<SigningKey, ReturnState>>> wait = new LinkedList<>();
        Map<SigningKey, ReturnState> results = new HashMap<>();

        // First run all the machines.
        for (BlackBox machine : machines.values()) {
            // TODO allow for the machines to be started in various orders.
            Future<Map.Entry<SigningKey, ReturnState>> future = machine.doIt();
            wait.add(future);
        }

        // TODO Allow for timeouts.
        while (wait.size() != 0) {
            Iterator<Future<Map.Entry<SigningKey, ReturnState>>> i = wait.iterator();
            while (i.hasNext()) {
                Future<Map.Entry<SigningKey, ReturnState>> future = i.next();
                if (future.isDone()) {
                    try {
                        Map.Entry<SigningKey, ReturnState> sim = future.get();
                        results.put(sim.getKey(), sim.getValue());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }

                    i.remove();
                }
            }
        }

        return results;
    }

    // TODO allow the simulator to monitor all inbox and do malicious things with them.
    public void sendTo(VerificationKey to, Packet packet) throws InvalidImplementationError, InterruptedException {
        machines.get(to).deliver(packet);
    }

}
