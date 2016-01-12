package com.shuffle.protocol;

import com.shuffle.cryptocoin.Address;
import com.shuffle.cryptocoin.Coin;
import com.shuffle.cryptocoin.Crypto;
import com.shuffle.cryptocoin.CryptographyError;
import com.shuffle.cryptocoin.EncryptionKey;
import com.shuffle.cryptocoin.SigningKey;
import com.shuffle.cryptocoin.Transaction;
import com.shuffle.cryptocoin.VerificationKey;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    ConcurrentMap<VerificationKey, BlackBox> machines;
    //long amount;
    final MessageFactory messages;
    final Crypto crypto;

    // This implementation of Network connects each shuffle machine to the simulator.
    private class Network implements com.shuffle.protocol.Network {
        final BlockingQueue<Packet> inbox = new LinkedBlockingQueue<>();

        Network() {
        }

        @Override
        public void sendTo(VerificationKey to, Packet packet) throws InvalidImplementationError, TimeoutError {

            try {
                Simulator.this.sendTo(to, new Packet(messages.copy(packet.message), packet.session, packet.phase, packet.signer, packet.recipient));
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

    public interface MockCoin extends Coin {
        void put(Address addr, long value);
        Transaction spend(Address from, Address to, int amount);
    }

    // Could be a real or a malicious player.
    public interface Adversary {
        SessionIdentifier session();
        ReturnState turnOn() throws InvalidImplementationError;
        Phase currentPhase();
        void deliver(Packet packet) throws InterruptedException;
        SigningKey identity();
    }

    // A wraper for the regular version of the protocol.
    public class HonestAdversary implements Adversary {
        final CoinShuffle.ShuffleMachine machine;
        final Network network;
        final SigningKey sk;
        final SessionIdentifier session;

        HonestAdversary(SessionIdentifier session,
                        long amount,
                        SigningKey sk,
                        SortedSet<VerificationKey> players,
                        Coin coin) {
            this.session = session;
            this.sk = sk;
            this.network = new Network();
            this.machine = new CoinShuffle(messages, crypto, coin, network).new ShuffleMachine(session, amount, sk, players, null, 1, 2);
        }

        @Override
        public SessionIdentifier session() {
            return session;
        }

        @Override
        public ReturnState turnOn() throws InvalidImplementationError {
            try {
                return machine.run();
            } catch (InterruptedException e) {
                return new ReturnState(false, session, machine.currentPhase(), e, null);
            }
        }

        @Override
        public Phase currentPhase() {
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

    public interface MessageReplacement {
        // Replace a message with a malicious message.
        Packet replace(Packet packet) throws FormatException;
    }

    // The malicious adversary can be made to send malicious messages or to spend bitcoins at
    // inappropriate times.
    public class MaliciousAdversary implements Adversary{
        // This implementation of Network connects each shuffle machine to the simulator.
        private class MaliciousNetwork extends Network {
            final MessageReplacement malicious;

            MaliciousNetwork(MessageReplacement malicious) {
                this.malicious = malicious;
            }

            @Override
            public void sendTo(VerificationKey to, Packet packet) throws InvalidImplementationError, TimeoutError {
                // Replace with malicious packet if necessary.
                try {
                    super.sendTo(to, malicious.replace(packet));
                } catch (FormatException e) {
                    e.printStackTrace();
                }
            }
        }

        final SessionIdentifier session;
        final CoinShuffle.ShuffleMachine machine;
        final Network network;
        final SigningKey sk;

        final Transaction t;
        final Coin coin;
        boolean transactionSent = false;

        MaliciousAdversary(
                SessionIdentifier session,
                long amount,
                SigningKey sk,
                SortedSet<VerificationKey> players,
                Coin coin,
                Crypto crypto,
                final MessageReplacement lies,
                Transaction t) {
            this.session = session;
            this.sk = sk;
            this.coin = coin;
            if (lies == null) {
                this.network = new Network();
            } else {
                this.network = new MaliciousNetwork(lies);
            }
            this.t = t;
            this.machine = new CoinShuffle(messages, crypto, coin, network).new ShuffleMachine(session, amount, sk, players, null, 1, 2);
        }

        // A player sends different encryption keys to different players.
        public class EquivocateEncryptionKeys implements MessageReplacement {
            final Set<VerificationKey> others;
            final Packet alternate;

            public EquivocateEncryptionKeys(Set<VerificationKey> others, EncryptionKey alternate) {
                this.others = others;
                this.alternate = null; // TODO
            }

            @Override
            public Packet replace(Packet packet) {
                if (packet.phase == Phase.Announcement) {
                    if (others.contains(packet.recipient)) {
                        return alternate;
                    }
                }

                return packet;
            }
        }

        // Send different output address to different players.
        public class EquivocateOutputVector implements MessageReplacement {
            Set<VerificationKey> others;
            Packet alternate;

            @Override
            public Packet replace(Packet packet) {
                if (packet.phase == Phase.BroadcastOutput) {
                    if (others.contains(packet.recipient)) {
                        if (alternate == null) {
                            // Reshuffle the packet we just got.
                            // TODO
                        }
                        return alternate;
                    }
                }

                return packet;
            }
        }

        // Drop an address in phase 2.
        public class DropAddress implements MessageReplacement {

            @Override
            public Packet replace(Packet packet) throws FormatException {
                if (packet.phase == Phase.Shuffling) {
                    // drop a random address from the packet.
                    List<Address> addresses = new LinkedList<>();
                    Message message = packet.message;

                    while (!message.isEmpty()) {
                        addresses.add(message.readAddress());
                    }

                    int n = crypto.getRandom(addresses.size() - 1);

                    int i = 0;
                    for (Address address : addresses) {
                        if (i != n) {
                            message.attach(address);
                        }
                    }
                }
                return packet;
            }
        }

        // Drop an address and replace with a duplicate in phase 2.
        public class DropAddressReplaceDuplicate implements MessageReplacement {
            public DropAddressReplaceDuplicate() {}

            @Override
            public Packet replace(Packet packet) throws FormatException {
                if (packet.phase == Phase.Shuffling) {
                    // drop a random address from the packet.
                    List<Address> addresses = new LinkedList<>();
                    Message message = packet.message;

                    while (!message.isEmpty()) {
                        addresses.add(message.readAddress());
                    }

                    int n = crypto.getRandom(addresses.size() - 1);
                    int m ;
                    do {
                        m = crypto.getRandom(addresses.size() - 1);
                    } while (m != n);

                    Address duplicate = addresses.get(m);

                    int i = 0;
                    for (Address address : addresses) {
                        if (i != n) {
                            message.attach(address);
                        } else {
                            message.attach(duplicate);
                        }
                    }
                }
                return packet;
            }
        }

        // Drop an address and replace with a new one in phase 2.
        public class DropAddressReplaceNew implements MessageReplacement {
            final Address alternate;

            public DropAddressReplaceNew(Address alternate) {
                this.alternate = alternate;
            }

            @Override
            public Packet replace(Packet packet) throws FormatException {
                if (packet.phase == Phase.Shuffling) {
                    // drop a random address from the packet.
                    List<Address> addresses = new LinkedList<>();
                    Message message = packet.message;

                    while (!message.isEmpty()) {
                        addresses.add(message.readAddress());
                    }

                    int n = crypto.getRandom(addresses.size() - 1);

                    int i = 0;
                    for (Address address : addresses) {
                        if (i != n) {
                            message.attach(address);
                        } else {
                            message.attach(alternate);
                        }
                    }
                }
                return packet;
            }
        }

        @Override
        public SessionIdentifier session() {
            return session;
        }

        @Override
        public ReturnState turnOn() throws InvalidImplementationError {
            try {
                machine.run();
                // Malicious players don't need to return any information since they are not being tested.
                return new ReturnState(false, session, Phase.Malicious, null, null);
            } catch (InterruptedException e) {
                return new ReturnState(false, session, machine.currentPhase(), e, null);
            }
        }

        @Override
        public Phase currentPhase() {
            return machine.currentPhase();
        }

        @Override
        public void deliver(Packet packet) throws InterruptedException {
            // Part way through the protocol, send the malicious bitcoin transaction. 
            if (packet.phase == Phase.BroadcastOutput && !transactionSent) {
                t.send();
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
                q.add(new ReturnState(false, machine.session(), machine.currentPhase(), e, null));
            }
        }
    }

    // TODO allow the simulator to monitor all inbox and do malicious things with them.
    public void sendTo(VerificationKey to, Packet packet) throws InvalidImplementationError, InterruptedException {
        machines.get(to).deliver(packet);
    }

    public Simulator(MessageFactory messages, Crypto crypto)  {
        this.messages = messages;
        this.crypto = crypto;
    }

    public LinkedHashMap<SigningKey, ReturnState> runSimulation(
            long amount,
            List<Adversary> init)  {
        if (init == null ) throw new NullPointerException();

        machines = new ConcurrentHashMap<>();

        int i = 0;
        try {
            for (Adversary in : init) {
                machines.put(in.identity().VerificationKey(),
                        new BlackBox(in));
                i++;
            }
        } catch (CryptographyError e) {
            e.printStackTrace();
        }

        //Timer timer = new Timer();
        List<Future<Map.Entry<SigningKey, ReturnState>>> wait = new LinkedList<>();
        LinkedHashMap<SigningKey, ReturnState> results = new LinkedHashMap<>();

        // First run all the machines.
        for (BlackBox machine : machines.values()) {
            // TODO allow for the machines to be started in various orders.
            Future<Map.Entry<SigningKey, ReturnState>> future = machine.doIt();
            wait.add(future);
        }

        System.out.println("Simulation running. " + wait.size() + " futures.");

        // TODO Allow for timeouts.
        while (wait.size() != 0) {
            Iterator<Future<Map.Entry<SigningKey, ReturnState>>> I = wait.iterator();
            while (I.hasNext()) {
                Future<Map.Entry<SigningKey, ReturnState>> future = I.next();
                if (future.isDone()) {
                    try {
                        Map.Entry<SigningKey, ReturnState> sim = future.get();
                        results.put(sim.getKey(), sim.getValue());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }

                    I.remove();
                }
            }
        }

        return results;
    }

    public LinkedHashMap<SigningKey, ReturnState> successfulRun(
            SessionIdentifier session,
            int numPlayers,
            long amount,
            MockCoin coin
    ) {

        SortedSet<VerificationKey> players = new TreeSet<>();
        List<SigningKey> keys = new LinkedList<>();

        for (int i = 1; i <= numPlayers; i++) {
            SigningKey key = crypto.makeSigningKey();
            players.add(key.VerificationKey());
            keys.add(key);
        }

        List<Adversary> init = new LinkedList<>();

        for (SigningKey key : keys) {
            Address address = key.VerificationKey().address();
            coin.put(address, 20);
            init.add(new HonestAdversary(session, amount, key, players, coin));
        }

        return runSimulation(numPlayers, init);
    }

    public LinkedHashMap<SigningKey, ReturnState> insufficientFundsRun(
            SessionIdentifier session,
            int numPlayers,
            int[] deadbeats, // Players who put no money in their address. 
            int[] poor, // Players who didn't put enough in their address.
            int[] spenders, // Players who don't have enough because they spent it.
            long amount,
            MockCoin coin
    ) {

        SortedSet<Integer> deadbeatSet = new TreeSet<>();
        SortedSet<Integer> spendersSet = new TreeSet<>();
        SortedSet<Integer> poorSet = new TreeSet<>();

        for(int i = 0; i < deadbeats.length; i++) {
            deadbeatSet.add(deadbeats[i]);
        }

        for(int i = 0; i < poor.length; i++) {
            if(!deadbeatSet.contains(poor[i])) {
                poorSet.add(poor[i]);
            }
        }

        for(int i = 0; i < spenders.length; i++) {
            if(!deadbeatSet.contains(spenders[i])
                    && !poorSet.contains(spenders[i])) {
                spendersSet.add(spenders[i]);
            }
        }

        Set<SigningKey> deadbeatKeys = new HashSet<>();
        Set<SigningKey> spendersKeys = new HashSet<>();
        Set<SigningKey> poorKeys = new HashSet<>();

        SortedSet<VerificationKey> players = new TreeSet<>();
        List<SigningKey> keys = new LinkedList<>();

        for (int i = 1; i <= numPlayers; i++) {
            SigningKey key = crypto.makeSigningKey();
            players.add(key.VerificationKey());
            keys.add(key);

            if (deadbeatSet.contains(i)) {
                deadbeatKeys.add(key);
            } else if (poorSet.contains(i)) {
                poorKeys.add(key);
            } if (spendersSet.contains(i)) {
                spendersKeys.add(key);
            }
        }

        for (SigningKey key : keys) {
            Address previousAddress = crypto.makeSigningKey().VerificationKey().address();

            // Each player starts with enough money.
            coin.put(previousAddress, 20);

            Address address = key.VerificationKey().address();

            if (poorKeys.contains(key)) {
                // Not enough money is in the poor person's address.
                coin.spend(previousAddress, address, 10).send();
            } else if (spendersKeys.contains(key)) {
                // We put enough money in the address.
                coin.spend(previousAddress, address, 20).send();

                // Plot twist! We spend it all!
                coin.spend(address, crypto.makeSigningKey().VerificationKey().address(), 20).send();
            } else if (!deadbeatKeys.contains(key)) {
                // This is a normal, not cheating player. Deadbeat players
                // don't have anything, so we don't send anything to their address.
                coin.spend(previousAddress, address, 20).send();
            }

        }

        List<Adversary> init = new LinkedList<>();

        for (SigningKey key : keys) {
            init.add(new HonestAdversary(session, amount, key, players, coin));
        }

        return runSimulation(amount, init);
    }

    public LinkedHashMap<SigningKey, ReturnState> doubleSpendingRun(
            SessionIdentifier session,
            int[] views,
            Map<Integer, MockCoin> coinNet,
            int[] doubleSpenders,
            long amount
    ) {
        SortedSet<Integer> doubleSpendSet = new TreeSet<>();
        for(int i = 0; i < doubleSpenders.length; i++) {
            doubleSpendSet.add(doubleSpenders[i]);
        }

        Set<SigningKey> doubleSpendKeys = new HashSet<>();

        List<SigningKey> keys = new LinkedList<>();
        Map<SigningKey, MockCoin> playerToCoin = new HashMap<>();
        SortedSet<VerificationKey> players = new TreeSet<>();

        for(int i = 0; i < views.length; i ++) {
            SigningKey key = crypto.makeSigningKey();
            keys.add(key);
            playerToCoin.put(key, coinNet.get(views[i]));

            if (doubleSpendSet.contains(i)) {
                doubleSpendKeys.add(key);
            }

            players.add(key.VerificationKey());
        }

        List<Adversary> init = new LinkedList<>();

        // Set up the networks with everyone having the correct initial amounts.
        for (SigningKey key : keys) {
            Address previousAddress = crypto.makeSigningKey().VerificationKey().address();
            Address address = key.VerificationKey().address();

            for (MockCoin coin : coinNet.values()) {
                coin.put(previousAddress, 20);
                coin.spend(previousAddress, address, 20).send();
            }

            // Make double spend transaction if applicable.
            if (doubleSpendKeys.contains(key)) {
                init.add(new MaliciousAdversary(session, amount, key, players, playerToCoin.get(key), crypto, null,
                        playerToCoin.get(key).spend(address, crypto.makeSigningKey().VerificationKey().address(), 16)));
            } else {
                init.add(new HonestAdversary(session, amount, key, players, playerToCoin.get(key)));
            }
        }

        return runSimulation(amount, init);
    }
}
