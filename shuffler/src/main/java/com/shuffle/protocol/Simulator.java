package com.shuffle.protocol;

import com.shuffle.cryptocoin.Address;
import com.shuffle.cryptocoin.Coin;
import com.shuffle.cryptocoin.Crypto;
import com.shuffle.cryptocoin.CryptographyError;
import com.shuffle.cryptocoin.EncryptionKey;
import com.shuffle.cryptocoin.SigningKey;
import com.shuffle.cryptocoin.Transaction;
import com.shuffle.cryptocoin.VerificationKey;

import java.util.ArrayList;
import java.util.Deque;
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
        Transaction spend(Address from, Address to, long amount);
    }

    public interface MessageReplacement {
        // Replace a message with a malicious message.
        Packet replace(Packet packet) throws FormatException;
    }

    // The malicious adversary can be made to send malicious messages or to spend bitcoins at
    // inappropriate times.
    private class Adversary {
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

        Adversary(
                SessionIdentifier session,
                long amount,
                SigningKey sk,
                SortedSet<VerificationKey> players,
                Coin coin,
                Transaction t,
                final MessageReplacement lies) {
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

        public SessionIdentifier session() {
            return session;
        }

        public ReturnState turnOn() throws InvalidImplementationError {
            try {
                return machine.run();
            } catch (InterruptedException e) {
                return new ReturnState(false, session, machine.currentPhase(), e, null);
            }
        }

        public Phase currentPhase() {
            return machine.currentPhase();
        }

        public void deliver(Packet packet) throws InterruptedException {
            // Part way through the protocol, send the malicious bitcoin transaction. 
            if (packet.phase == Phase.EquivocationCheck && !transactionSent && t != null) {
                t.send();
                transactionSent = true;
            }
            network.deliver(packet);
        }

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

    public void sendTo(VerificationKey to, Packet packet) throws InvalidImplementationError, InterruptedException {
        machines.get(to).deliver(packet);
    }

    public Simulator(MessageFactory messages, Crypto crypto)  {
        this.messages = messages;
        this.crypto = crypto;
    }

    private synchronized LinkedHashMap<SigningKey, ReturnState> runSimulation(
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

    public class InitialState {
        private final SessionIdentifier session;
        private final long amount;
        private final Deque<Player> players = new LinkedList<>();
        private MockCoin defaultCoin = null;

        private class Player {
            long initialAmount = 0;
            long spend = 0;
            long doubleSpend = 0;
            MockCoin coin = null;
            boolean change = false; // Whether to make a change address.

            int[] equivocateAnnouncement = new int[]{};
            int[] equivocateBroadcast = new int[]{};
            int drop = 0; // Whether to drop an address in phase 2.
            int duplicate = 0; // Whether to duplicate another address and replace it with the dropped address.
            boolean replace = false; // Whether to replace dropped address with a new one.

            Player() {}

        }

        private InitialState(SessionIdentifier session, long amount) {

            this.session = session;
            this.amount = amount;
        }

        public InitialState defaultCoin(MockCoin coin) {
            defaultCoin = coin;
            return this;
        }

        public InitialState player() {
            players.add(new Player());
            return this;
        }

        InitialState initialFunds(long amount) {
            players.getLast().initialAmount = amount;
            return this;
        }

        InitialState spend(long amount) {
            players.getLast().spend = amount;
            return this;
        }

        InitialState doubleSpend(long amount) {
            players.getLast().doubleSpend = amount;
            return this;
        }

        InitialState coin(MockCoin coin) {
            players.getLast().coin = coin;
            return this;
        }

        InitialState equivocateAnnouncement(int[] equivocate) {
            players.getLast().equivocateAnnouncement = equivocate;
            return this;
        }

        InitialState equivocateBroadcast(int[] equivocate) {
            players.getLast().equivocateBroadcast = equivocate;
            return this;
        }

        InitialState change() {
            players.getLast().change = true;
            return this;
        }

        InitialState drop(int drop) {
            players.getLast().drop = drop;
            players.getLast().duplicate = 0;
            players.getLast().replace = false;
            return this;
        }

        InitialState replace(int drop, int duplicate) {
            players.getLast().drop = drop;
            players.getLast().duplicate = duplicate;
            players.getLast().replace = false;
            return this;
        }

        InitialState replace(int drop) {
            players.getLast().drop = drop;
            players.getLast().duplicate = 0;
            players.getLast().replace = true;
            return this;
        }

        public LinkedHashMap<SigningKey, ReturnState> run() {
            List<Adversary> adversaries = new LinkedList<>();
            Map<Player, SigningKey> keys = new HashMap<>();

            for(Player player : players) {
                keys.put(player, crypto.makeSigningKey());
            }

            // Check that all players have a coin network set up, either the default or their own.
            for(Player player : players) {
                MockCoin coin;
                if(player.coin != null) {
                    coin = player.coin;
                } else if (defaultCoin != null) {
                    coin = defaultCoin;
                } else {
                    return null;
                }

                SortedSet<VerificationKey> identities = new TreeSet<>();

                for (SigningKey key : keys.values()) {
                    identities.add(key.VerificationKey());
                }

                SigningKey key = keys.get(player);
                Address address = key.VerificationKey().address();
                Transaction doubleSpend = null;

                // Set up the player's initial funds.
                if (player.initialAmount > 0) {
                    Address previousAddress = crypto.makeSigningKey().VerificationKey().address();
                    coin.put(previousAddress, player.initialAmount);
                    coin.spend(previousAddress, address, player.initialAmount).send();

                    // Plot twist! We spend it all!
                    if (player.spend > 0) {
                        coin.spend(address, crypto.makeSigningKey().VerificationKey().address(), player.spend).send();
                    } else if(player.doubleSpend > 0) {
                        // is he going to double spend? If so, make a new transaction for him.
                        doubleSpend = player.coin.spend(address, crypto.makeSigningKey().VerificationKey().address(), 16);
                    }
                }

                // TODO Message replacements.

                adversaries.add(new Adversary(session, amount, key, identities, coin, doubleSpend, null));
            }

            return Simulator.this.runSimulation(amount, adversaries);
        }
    }

    public InitialState initialize(SessionIdentifier session, long amount) {
        return new InitialState(session, amount);
    }

    public LinkedHashMap<SigningKey, ReturnState> successfulRun(
            SessionIdentifier session,
            int numPlayers,
            long amount,
            MockCoin coin
    ) {

        InitialState init = initialize(session, amount).defaultCoin(coin);

        for (int i = 1; i <= numPlayers; i++) {
            init.player().initialFunds(20);
        }

        return init.run();
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
        InitialState init = initialize(session, amount).defaultCoin(coin);

        for (int i = 1; i <= numPlayers; i++) {
            init.player().initialFunds(20);
            for (int j = 0; j < deadbeats.length; j++) {
                if (deadbeats[j] == i) {
                    init.initialFunds(0);
                }
            }
            for (int j = 0; j < poor.length; j++) {
                if (poor[j] == i) {
                    init.initialFunds(10);
                }
            }
            for (int j = 0; j < spenders.length; j++) {
                if (spenders[j] == i) {
                    init.spend(16);
                }
            }
        }

        return init.run();
    }

    public LinkedHashMap<SigningKey, ReturnState> doubleSpendingRun(
            SessionIdentifier session,
            Set<MockCoin> coinNets,
            List<MockCoin> coinNetList,
            int[] doubleSpenders,
            long amount
    ) {
        InitialState init = initialize(session, amount);

        int i = 1;
        for (MockCoin coinNet : coinNetList) {
            init.player().initialFunds(20).coin(coinNet);

            for (int j = 0; j < doubleSpenders.length; j++) {
                if (doubleSpenders[j] == i) {
                    init.spend(16);
                }
            }
            i++;
        }

        return init.run();
    }

    public LinkedHashMap<SigningKey, ReturnState> runWithReplacements(
            SessionIdentifier session,
            int numPlayers,
            long amount,
            MockCoin coin,
            Map<Integer, MessageReplacement> malicious
    ) {

        SortedSet<VerificationKey> players = new TreeSet<>();
        List<SigningKey> keys = new LinkedList<>();
        Map<SigningKey, MessageReplacement> maliciousPlayers = new HashMap<>();

        for (int i = 1; i <= numPlayers; i++) {
            SigningKey key = crypto.makeSigningKey();
            players.add(key.VerificationKey());
            keys.add(key);

            if (malicious.containsKey(i)) {
                maliciousPlayers.put(key, malicious.get(i));
            }
        }

        List<Adversary> init = new LinkedList<>();

        for (SigningKey key : keys) {
            Address address = key.VerificationKey().address();
            coin.put(address, 20);

            if (maliciousPlayers.containsKey(key)) {
                init.add(new Adversary(session, amount, key, players, coin, null, maliciousPlayers.get(key)));
            } else {
                init.add(new Adversary(session, amount, key, players, coin, null, null));
            }
        }

        return runSimulation(amount, init);
    }
}
