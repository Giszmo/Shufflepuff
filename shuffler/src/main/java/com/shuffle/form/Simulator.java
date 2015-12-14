package com.shuffle.form;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
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
public class Simulator {
    ConcurrentMap<VerificationKey, BlackBox> machines;
    VerificationKey players[];
    SessionIdentifier τ;
    CoinAmount ν;

    // This implementation of Network connects each shuffle machine to the simulator.
    private class Network implements com.shuffle.form.Network {
        BlockingQueue<Packet> messages;

        Network() {
            this.messages = new LinkedBlockingQueue<>();
        }

        @Override
        public void sendTo(VerificationKey to, Packet packet) throws InvalidImplementationException {
            Simulator.this.sendTo(to, packet);
        }

        @Override
        public Packet receive() throws TimeoutException, InterruptedException {
            Packet next = messages.poll(1, TimeUnit.SECONDS);
            if (next == null) {
                throw new TimeoutException();
            }
            return next;
        }

        public void deliver(Packet packet) throws  InterruptedException {
            messages.put(packet);
        }
    }

    // Simulations can have a pretty complicated initial state. This class is
    // for initializing a single machine in the simulation.
    public static class InitialState {
        public SigningKey key;
        public PacketFactory packets;
        public Crypto crypto;
        public Coin coin;

        public InitialState(SigningKey key, PacketFactory packets, Crypto crypto, Coin coin) {
            this.key = key;
            this.packets = packets;
            this.crypto = crypto;
            this.coin = coin;
        }
    }

    // Could be a real or a malicious player.
    private interface Adversary {
        ShuffleErrorState turnOn() throws InvalidImplementationException;
        ShufflePhase currentPhase();
        void deliver(Packet packet);
    }

    // A wraper for the regular version of the protocol.
    private class HonestAdversary implements Adversary {
        ShuffleMachine machine;
        Network network;

        SessionIdentifier τ;
        CoinAmount ν; SigningKey sk;
        VerificationKey[] players;

        HonestAdversary(SessionIdentifier τ, CoinAmount ν, SigningKey sk, VerificationKey[] players,
                PacketFactory packets, Crypto crypto, Coin coin) {
            this.τ = τ;
            this.ν = ν;
            this.sk = sk;
            this.players = players;
            this.network = new Network();
            this.machine = new ShuffleMachine(packets, crypto, coin, network);
        }

        @Override
        public ShuffleErrorState turnOn() throws InvalidImplementationException {
            try {
                ShuffleErrorState err = machine.run(τ, ν, sk, players);
                if (err == null) {
                    return new ShuffleErrorState(null, -1, null, null);
                }
                return err;
            } catch (InterruptedException e) {
                return new ShuffleErrorState(τ, -1, machine.currentPhase(), e);
            }
        }

        @Override
        public ShufflePhase currentPhase() {
            return machine.currentPhase();
        }

        @Override
        public void deliver(Packet packet) {

        }
    }

    // TODO!!! (Actually there might need to be several of these.)
    private class MaliciousAdversary implements Adversary{

        @Override
        public ShuffleErrorState turnOn() throws InvalidImplementationException {
            return null;
        }

        @Override
        public ShufflePhase currentPhase() {
            return null;
        }

        @Override
        public void deliver(Packet packet) {

        }
    }

    // A wrapper for an adversary that can run a player as a separate thread. Could be good or evil!
    private class BlackBox implements Runnable {
        Adversary machine;
        Thread thread;

        // Used to send a message from the new thread to the old one.
        BlockingQueue<ShuffleErrorState> q;

        public BlackBox(Adversary machine) {
            this.machine = machine;
            thread = null;
            q = new LinkedBlockingQueue<>();
        }

        public void deliver(Packet packet) throws InvalidImplementationException {
            machine.deliver(packet);
        }

        public void interrupt() {
            if (thread != null) {
                thread.interrupt();
            }
        }

        public Future<ShuffleErrorState> doIt() {
            // Start new thread.
            thread = new Thread(this);
            thread.start();

            // Wait for message from new thread.
            return new Future<ShuffleErrorState>() {
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
                public ShuffleErrorState get() throws InterruptedException, ExecutionException {
                    return q.take();
                }

                @Override
                public ShuffleErrorState get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {
                    return q.poll(l, timeUnit);
                }
            };
        }

        @Override
        public void run() {
            try {
                q.add(machine.turnOn());
            } catch (InvalidImplementationException e) {
                q.add(new ShuffleErrorState(τ, -1, machine.currentPhase(), e));
            }
        }
    }

    public Simulator(SessionIdentifier τ, CoinAmount ν, InitialState[] init)  {
        this.τ = τ;
        this.ν = ν;
        this.players = new VerificationKey[init.length];

        machines = new ConcurrentHashMap<>();

        int i = 0;
        try {
            for (InitialState in : init) {
                players[i] = in.key.VerificationKey();
                machines.put(in.key.VerificationKey(),
                        new BlackBox(
                                new HonestAdversary(τ, ν, in.key, players, in.packets, in.crypto, in.coin)));
                i++;
            }
        } catch (CryptographyException e) {
            e.printStackTrace();
        }

    }

    public List<ShuffleErrorState> runSimulation() {
        //Timer timer = new Timer();
        List<Future<ShuffleErrorState>> wait = new LinkedList<>();
        List<ShuffleErrorState> results = new LinkedList<>();

        // First run all the machines.
        for (BlackBox machine : machines.values()) {
            // TODO allow for the machines to be started in different orders.
            Future<ShuffleErrorState> future = machine.doIt();
            wait.add(future);
        }

        // TODO Allow for timeouts.
        while (wait.size() != 0) {
            Iterator<Future<ShuffleErrorState>> i = wait.iterator();
            while (i.hasNext()) {
                Future<ShuffleErrorState> future = i.next();
                if (future.isDone()) {
                    try {
                        results.add(future.get());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }

                    i.remove();
                }
            }
        }

        return results;
    }

    // TODO allow the simulator to monitor all messages and do malicious things with them.
    public void sendTo(VerificationKey to, Packet packet) throws InvalidImplementationException {
        machines.get(to).deliver(packet);
    }

}
