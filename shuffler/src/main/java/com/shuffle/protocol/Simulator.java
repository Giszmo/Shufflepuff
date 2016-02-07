package com.shuffle.protocol;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Coin;
import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
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
 * A simulator for running integration tests on the protocol.
 *
 * Created by Daniel Krawisz on 12/6/15.
 */
public final class Simulator {
    private static Logger log= LogManager.getLogger(Simulator.class);

    ConcurrentMap<VerificationKey, BlackBox> machines;
    //long amount;
    final MessageFactory messages;
    final Crypto crypto;

    final Map<Phase, Map<VerificationKey, Map<VerificationKey, Packet>>> sent = new HashMap<>();

    public interface MockCoin extends Coin {
        void put(Address addr, long value);
        Transaction spend(Address from, Address to, long amount);
    }

    // This implementation of Network connects each shuffle machine to the simulator.
    private class Network implements com.shuffle.protocol.Network {
        final BlockingQueue<SignedPacket> inbox = new LinkedBlockingQueue<>();

        Network() {
        }

        @Override
        public void sendTo(VerificationKey to, SignedPacket packet) throws InvalidImplementationError, TimeoutError {

            try {
                Simulator.this.sendTo(to, packet);
            } catch (InterruptedException e) {
                // This means that the thread running the machine we are delivering to has been interrupted.
                // This would look like a timeout if this were happening over a real network.
                throw new TimeoutError();
            }
        }

        @Override
        public SignedPacket receive() throws TimeoutError, InterruptedException {
            for (int i = 0; i < 2; i ++) {
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

    // The malicious adversary can be made to send malicious messages or to spend bitcoins at
    // inappropriate times.
    private class Adversary {

        final SessionIdentifier session;
        final CoinShuffle shuffle;
        final CoinShuffle.Machine machine;
        Network network;
        final SigningKey sk;
        final SortedSet<VerificationKey> players;

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
                CoinShuffle shuffle,
                CoinShuffle.Machine machine) {
            this.session = session;
            this.sk = sk;
            this.coin = coin;
            this.network = new Network();
            this.players = players;
            this.t = t;
            this.shuffle = shuffle;
            this.machine = machine;
        }

        public SessionIdentifier session() {
            return session;
        }

        public CoinShuffle.Machine turnOn() throws InvalidImplementationError {
            return shuffle.run(machine, null, network);
        }

        public Phase currentPhase() {
            return machine.phase();
        }

        public void deliver(SignedPacket packet) throws InterruptedException {
            // Part way through the protocol, send the malicious bitcoin transaction. 
            if (packet.payload.phase == Phase.EquivocationCheck && !transactionSent && t != null) {
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
        Adversary adversary;
        Thread thread;

        // Used to send a message from the new thread to the old one.
        BlockingQueue<CoinShuffle.Machine> q;

        public BlackBox(Adversary adversary) {
            this.adversary = adversary;
            thread = null;
            q = new LinkedBlockingQueue<>();
        }

        public void deliver(SignedPacket packet) throws InvalidImplementationError, InterruptedException {
            adversary.deliver(packet);
        }

        public void interrupt() {
            if (thread != null) {
                thread.interrupt();
            }
        }

        public Future<Map.Entry<SigningKey, CoinShuffle.Machine>> doIt() {
            class entry implements Map.Entry<SigningKey, CoinShuffle.Machine> {
                SigningKey key;
                CoinShuffle.Machine re;

                public entry(SigningKey key, CoinShuffle.Machine re) {
                    this.key = key;
                    this.re = re;
                }

                @Override
                public SigningKey getKey() {
                    return key;
                }

                @Override
                public CoinShuffle.Machine getValue() {
                    return re;
                }

                @Override
                public CoinShuffle.Machine setValue(CoinShuffle.Machine machine) {
                    re = machine;
                    return re;
                }
            }

            // Start new thread.
            thread = new Thread(this);
            thread.start();

            // Wait for message from new thread.
            return new Future<Map.Entry<SigningKey, CoinShuffle.Machine>>() {
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
                public Map.Entry<SigningKey, CoinShuffle.Machine> get() throws InterruptedException, ExecutionException {
                    return new entry(adversary.identity(), q.take());
                }

                @Override
                public Map.Entry<SigningKey, CoinShuffle.Machine> get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {
                    return new entry(adversary.identity(), q.poll(l, timeUnit));
                }
            };
        }

        @Override
        public void run() {
            try {
                q.add(adversary.turnOn());
            } catch (InvalidImplementationError e) {
                q.add(adversary.machine);
            }
        }
    }

    public void sendTo(VerificationKey to, SignedPacket packet) throws InvalidImplementationError, InterruptedException {
        Map<VerificationKey, Map<VerificationKey, Packet>> byPacket = sent.get(packet.payload.phase);
        if(byPacket == null) {
            byPacket = new HashMap<>();
            sent.put(packet.payload.phase, byPacket);
        }

        Map<VerificationKey, Packet> bySender = byPacket.get(packet.payload.signer);
        if (bySender == null) {
            bySender = new HashMap<>();
            byPacket.put(packet.payload.signer, bySender);
        }

        bySender.put(packet.payload.recipient, packet.payload);

        machines.get(to).deliver(packet);
    }

    public Simulator(MessageFactory messages, Crypto crypto)  {
        this.messages = messages;
        this.crypto = crypto;
    }

    private synchronized Map<SigningKey, CoinShuffle.Machine> runSimulation(
            List<Adversary> init)  {
        if (init == null ) throw new NullPointerException();

        machines = new ConcurrentHashMap<>();
        sent.clear();

        try {
            for (Adversary in : init) {
                machines.put(in.identity().VerificationKey(),
                        new BlackBox(in));
            }
        } catch (CryptographyError e) {
            log.error("Some Crypto error happened",e);
        }

        //Timer timer = new Timer();
        List<Future<Map.Entry<SigningKey, CoinShuffle.Machine>>> wait = new LinkedList<>();
        Map<SigningKey, CoinShuffle.Machine> results = new HashMap<>();

        // First run all the machines.
        for (BlackBox machine : machines.values()) {
            // TODO allow for the machines to be started in various orders.
            Future<Map.Entry<SigningKey, CoinShuffle.Machine>> future = machine.doIt();
            wait.add(future);
        }

        // TODO Allow for timeouts.
        while (wait.size() != 0) {
            Iterator<Future<Map.Entry<SigningKey, CoinShuffle.Machine>>> I = wait.iterator();
            while (I.hasNext()) {
                Future<Map.Entry<SigningKey, CoinShuffle.Machine>> future = I.next();
                if (future.isDone()) {
                    try {
                        Map.Entry<SigningKey, CoinShuffle.Machine> sim = future.get();
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
        private List<MockCoin> coins;

        private class Player {
            long initialAmount = 0;
            long spend = 0;
            long doubleSpend = 0;
            MockCoin coin = null;
            boolean change = false; // Whether to make a change address.

            // Whether the adversary should equivocate during the announcement phase and to whom.
            int[] equivocateAnnouncement = new int[]{};

            // Whether the adversary should equivocate during the broadcast phase and to whom.
            int[] equivocateOutputVector = new int[]{};

            int drop = 0; // Whether to drop an address in phase 2.
            int duplicate = 0; // Whether to duplicate another address and replace it with the dropped address.
            boolean replace = false; // Whether to replace dropped address with a new one.

            Player() {}

            Adversary adversary(Map<Player, SigningKey> keys) {
                MockCoin newCoin;
                if(coin != null) {
                    newCoin = coin;
                } else if (defaultCoin != null) {
                    newCoin = defaultCoin;
                } else {
                    return null;
                }

                List<MockCoin> coins;
                if (InitialState.this.coins != null) {
                    coins = InitialState.this.coins;
                } else {
                    coins = new LinkedList<>();
                    coins.add(newCoin);
                }

                SortedSet<VerificationKey> identities = new TreeSet<>();

                for (SigningKey key : keys.values()) {
                    identities.add(key.VerificationKey());
                }

                SigningKey key = keys.get(this);
                Address address = key.VerificationKey().address();
                Transaction doubleSpendTrans = null;

                // Set up the player's initial funds.
                if (initialAmount > 0) {
                    Address previousAddress = crypto.makeSigningKey().VerificationKey().address();

                    for (MockCoin coin : coins) {
                        coin.put(previousAddress, initialAmount);
                        coin.spend(previousAddress, address, initialAmount).send();

                        // Plot twist! We spend it all!
                        if (spend > 0) {
                            coin.spend(address, crypto.makeSigningKey().VerificationKey().address(), spend).send();
                        }
                    }

                    if (doubleSpend > 0) {
                        // is he going to double spend? If so, make a new transaction for him.
                        doubleSpendTrans = newCoin.spend(address, crypto.makeSigningKey().VerificationKey().address(), doubleSpend);
                    }
                }

                CoinShuffle shuffle;
                CoinShuffle.Machine machine = new CoinShuffle.Machine(session, amount, key, identities);

                if (equivocateAnnouncement != null && equivocateAnnouncement.length > 0) {
                    shuffle = MaliciousMachine.announcementEquivocator(messages, crypto, newCoin, fromSet(identities, equivocateAnnouncement));
                } else if (equivocateOutputVector != null && equivocateOutputVector.length > 0) {
                    shuffle = MaliciousMachine.broadcastEquivocator(messages, crypto, newCoin, fromSet(identities, equivocateOutputVector));
                } else if (replace && drop != 0) {
                    shuffle = MaliciousMachine.addressDropperReplacer(messages, crypto, newCoin, drop);
                } else if (duplicate != 0 && drop != 0) {
                    shuffle = MaliciousMachine.addressDropperDuplicator(messages, crypto, newCoin, drop, duplicate);
                } else if (drop != 0) {
                    shuffle = MaliciousMachine.addressDropper(messages, crypto, newCoin, drop);
                } else {
                    shuffle = new CoinShuffle(messages, crypto, newCoin);
                }

                return new Adversary(session, amount, key, identities, newCoin, doubleSpendTrans, shuffle, machine);
            }
        }

        private InitialState(SessionIdentifier session, long amount) {

            this.session = session;
            this.amount = amount;
        }

        public InitialState defaultCoin(MockCoin coin) {
            defaultCoin = coin;
            return this;
        }

        public InitialState coins(List<MockCoin> coins) {
            this.coins = coins;
            return this;
        }

        public InitialState player() {
            players.addLast(new Player());
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

        InitialState equivocateOutputVector(int[] equivocate) {
            players.getLast().equivocateOutputVector = equivocate;
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

        public Map<SigningKey, CoinShuffle.Machine> run() {
            List<Adversary> adversaries = new LinkedList<>();
            Map<Player, SigningKey> keys = new HashMap<>();

            for(Player player : players) {
                keys.put(player, crypto.makeSigningKey());
            }

            // Check that all players have a coin network set up, either the default or their own.
            for(Player player : players) {
                Adversary adversary = player.adversary(keys);
                if (adversary == null) {
                    return null;
                }
                adversaries.add(adversary);
            }

            return Simulator.this.runSimulation(adversaries);
        }

        private Set<VerificationKey> fromSet(SortedSet<VerificationKey> identities, int[] array) {
            Set<VerificationKey> others = new TreeSet<>();

            int p = 1;
            int i = 0;
            for(VerificationKey player: identities) {
                while(i < array.length && array[i] < p) {
                    i++;
                }

                if(i < array.length && array[i] == p) {
                    others.add(player);
                }

                p ++;
            }

            return others;
        }
    }

    public InitialState initialize(SessionIdentifier session, long amount) {
        return new InitialState(session, amount);
    }

    public Map<SigningKey, CoinShuffle.Machine> successfulRun(
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

    public Map<SigningKey, CoinShuffle.Machine> insufficientFundsRun(
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
            for (int deadbeat : deadbeats) {
                if (deadbeat == i) {
                    init.initialFunds(0);
                }
            }
            for (int aPoor : poor) {
                if (aPoor == i) {
                    init.initialFunds(10);
                }
            }
            for (int spender : spenders) {
                if (spender == i) {
                    init.spend(16);
                }
            }
        }

        return init.run();
    }

    public Map<SigningKey, CoinShuffle.Machine> doubleSpendingRun(
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

            for (int doubleSpender : doubleSpenders) {
                if (doubleSpender == i) {
                    init.spend(16);
                }
            }
            i++;
        }

        return init.run();
    }
}
