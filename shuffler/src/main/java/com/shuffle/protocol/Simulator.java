package com.shuffle.protocol;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Coin;
import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    public interface MessageReplacement {
        // Replace a message with a malicious message.
        SignedPacket replace(SignedPacket packet) throws FormatException;
    }

    // This implementation of Network connects each shuffle machine to the simulator.
    private class Network implements com.shuffle.protocol.Network {
        MessageReplacement malicious; // Can be used to replace messages with malicious ones.
        final BlockingQueue<SignedPacket> inbox = new LinkedBlockingQueue<>();

        Network() {
        }

        public Network addReplacement(MessageReplacement replacement) {

            if (replacement == null) {
                return this;
            }

            if (malicious == null) {
                malicious = replacement;
            } else {
                malicious = new Composition(replacement, malicious);
            }

            return this;
        }

        @Override
        public void sendTo(VerificationKey to, SignedPacket packet) throws InvalidImplementationError, TimeoutError {

            // Replace with malicious packet if necessary.
            if (malicious != null) {
                try {
                    packet = malicious.replace(packet);
                } catch (FormatException e) {
                    log.error("Error sending ", e);
                }
            }

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

    public class Composition implements MessageReplacement {
        MessageReplacement first;
        MessageReplacement rest;

        public Composition(MessageReplacement first, MessageReplacement rest) {
            this.first = first;
            this.rest = rest;
        }

        @Override
        public SignedPacket replace(SignedPacket packet) throws FormatException {
            return rest.replace(first.replace(packet));
        }
    }

    // The malicious adversary can be made to send malicious messages or to spend bitcoins at
    // inappropriate times.
    private class Adversary {

        final SessionIdentifier session;
        final CoinShuffle shuffle;
        final CoinShuffle.ShuffleMachine machine;
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
                Transaction t) {
            this.session = session;
            this.sk = sk;
            this.coin = coin;
            this.network = new Network();
            this.players = players;
            this.t = t;
            shuffle = new CoinShuffle(messages, crypto, coin);
            this.machine = shuffle.new ShuffleMachine(session, amount, sk, players, null, 1, 2);
        }

        Adversary lie(MessageReplacement lie) {
            network.addReplacement(lie);
            return this;
        }

        // A player sends different encryption keys to different players.
        // TODO malicious player needs to eqivocate again in phase 4 to stay consistent.
        public class EquivocateEncryptionKeys implements MessageReplacement {
            final Set<VerificationKey> others;
            final EncryptionKey alternate;

            public EquivocateEncryptionKeys(int[] others) {
                this.others = new TreeSet<>();

                int p = 1;
                int i = 0;
                for(VerificationKey player: players) {
                    while(i < others.length && others[i] < p) {
                        i++;
                    }

                    if(i < others.length && others[i] == p) {
                        this.others.add(player);
                    }

                    p ++;
                }
                alternate = crypto.makeDecryptionKey().EncryptionKey();
            }

            @Override
            public SignedPacket replace(SignedPacket sigPacket) {
                Packet packet = sigPacket.payload;

                if (packet.phase == Phase.Announcement && others.contains(packet.recipient)) {
                    Message message = packet.message;

                    // Sometimes a change address is included with message 1.
                    Address change = null;
                    try {
                        message = message.rest();
                        if (!message.isEmpty()) {
                            change = message.readAddress();
                        }
                    } catch (FormatException e) {
                        e.printStackTrace();
                    }

                    message = message.attach(alternate);

                    if (change != null) {
                        message = message.attach(change);
                    }

                    Packet newPacket = new Packet(message, packet.session, packet.phase, packet.signer, packet.recipient);
                    return new SignedPacket(newPacket, sk.makeSignature(newPacket));
                }

                return sigPacket;
            }
        }

        // Send different output address to different players.
        // TODO malicious player needs to eqivocate again in phase 4 to stay consistent.
        public class EquivocateOutputVector implements MessageReplacement {
            Set<VerificationKey> others;
            Message alternate;

            public EquivocateOutputVector(int[] others) {
                this.others = new TreeSet<>();

                int p = 1;
                int i = 0;
                for(VerificationKey player: players) {
                    while(i < others.length && others[i] < p) {
                        i++;
                    }

                    if(i < others.length && others[i] == p) {
                        this.others.add(player);
                    }

                    p ++;
                }
            }

            @Override
            public SignedPacket replace(SignedPacket packet) {
                if (packet.payload.phase == Phase.BroadcastOutput && others.contains(packet.payload.recipient)) {
                    if (alternate == null) {
                        // Reshuffle the packet we just got.
                        try {
                            alternate = shuffle.shuffle(packet.payload.message);
                        } catch (FormatException e) {
                            e.printStackTrace();
                        }
                    }

                    Packet newPacket = new Packet(alternate, packet.payload.session, packet.payload.phase, packet.payload.signer, packet.payload.recipient);
                    return new SignedPacket(newPacket, sk.makeSignature(newPacket));
                }

                return packet;
            }
        }

        // Drop an address in phase 2.
        public class DropAddress implements MessageReplacement {
            int drop;

            public DropAddress(int drop) {
                this.drop = drop;
            }

            @Override
            public SignedPacket replace(SignedPacket sigPacket) throws FormatException {
                Packet packet = sigPacket.payload;

                if (packet.phase == Phase.Shuffling) {
                    // drop a random address from the packet.
                    List<Address> addresses = new LinkedList<>();
                    Message message = packet.message;

                    while (!message.isEmpty()) {
                        addresses.add(message.readAddress());
                        message = message.rest();
                    }

                    int i = 0;
                    for (Address address : addresses) {
                        if (i != drop) {
                            message = message.attach(address);
                        }
                    }

                    Packet newPacket = new Packet(message, packet.session, packet.phase, packet.signer, packet.recipient);
                    return new SignedPacket(newPacket, sk.makeSignature(newPacket));
                }
                return sigPacket;
            }
        }

        // Drop an address and replace with a duplicate in phase 2.
        public class DropAddressReplaceDuplicate implements MessageReplacement {
            int drop;
            int duplicate;

            public DropAddressReplaceDuplicate(int drop, int duplicate) {
                this.drop = drop;
                this.duplicate = duplicate;
            }

            @Override
            public SignedPacket replace(SignedPacket sigPacket) throws FormatException {
                Packet packet = sigPacket.payload;

                if (packet.phase == Phase.Shuffling) {
                    // drop a random address from the packet.
                    List<Address> addresses = new LinkedList<>();
                    Message message = packet.message;

                    while (!message.isEmpty()) {
                        addresses.add(message.readAddress());
                        message = message.rest();
                    }
                    Address duplicate = addresses.get(this.duplicate);

                    int i = 0;
                    for (Address address : addresses) {
                        if (i != drop) {
                            message = message.attach(address);
                        } else {
                            message = message.attach(duplicate);
                        }
                    }

                    Packet newPacket = new Packet(message, packet.session, packet.phase, packet.signer, packet.recipient);
                    return new SignedPacket(newPacket, sk.makeSignature(newPacket));
                }
                return sigPacket;
            }
        }

        // Drop an address and replace with a new one in phase 2.
        public class DropAddressReplaceNew implements MessageReplacement {
            final Address alternate;
            int drop;

            public DropAddressReplaceNew(int drop) {
                this.drop = drop;
                this.alternate = crypto.makeSigningKey().VerificationKey().address();
            }

            @Override
            public SignedPacket replace(SignedPacket sigPacket) throws FormatException {
                Packet packet = sigPacket.payload;
                if (packet.phase == Phase.Shuffling) {
                    // drop a random address from the packet.
                    List<Address> addresses = new LinkedList<>();
                    Message message = packet.message;

                    while (!message.isEmpty()) {
                        addresses.add(message.readAddress());
                    }

                    int i = 0;
                    for (Address address : addresses) {
                        if (i != drop) {
                            message.attach(address);
                        } else {
                            message.attach(alternate);
                        }
                    }

                    Packet newPacket = new Packet(message, packet.session, packet.phase, packet.signer, packet.recipient);
                    return new SignedPacket(newPacket, sk.makeSignature(newPacket));
                }
                return sigPacket;
            }
        }

        public SessionIdentifier session() {
            return session;
        }

        public ReturnState turnOn() throws InvalidImplementationError {
            try {
                return machine.run(network);
            } catch (InterruptedException e) {
                return new ReturnState(false, session, machine.currentPhase(), e, null);
            }
        }

        public Phase currentPhase() {
            return machine.currentPhase();
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
        Adversary machine;
        Thread thread;

        // Used to send a message from the new thread to the old one.
        BlockingQueue<ReturnState> q;

        public BlackBox(Adversary machine) {
            this.machine = machine;
            thread = null;
            q = new LinkedBlockingQueue<>();
        }

        public void deliver(SignedPacket packet) throws InvalidImplementationError, InterruptedException {
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

    private synchronized Map<SigningKey, ReturnState> runSimulation(
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
                MockCoin newcoin;
                if(coin != null) {
                    newcoin = coin;
                } else if (defaultCoin != null) {
                    newcoin = defaultCoin;
                } else {
                    return null;
                }

                List<MockCoin> coins;
                if (InitialState.this.coins != null) {
                    coins = InitialState.this.coins;
                } else {
                    coins = new LinkedList<>();
                    coins.add(newcoin);
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
                        doubleSpendTrans = newcoin.spend(address, crypto.makeSigningKey().VerificationKey().address(), doubleSpend);
                    }
                }

                Adversary adversary = new Adversary(session, amount, key, identities, newcoin, doubleSpendTrans);

                if (equivocateAnnouncement != null && equivocateAnnouncement.length > 0) {
                    adversary.lie(adversary.new EquivocateEncryptionKeys(equivocateAnnouncement));
                }

                if (equivocateOutputVector != null && equivocateOutputVector.length > 0) {
                    adversary.lie(adversary.new EquivocateOutputVector(equivocateOutputVector));
                }

                if (replace && drop != 0) {
                    adversary.lie(adversary.new DropAddressReplaceNew(drop));
                } else if (duplicate != 0 && drop != 0) {
                    adversary.lie(adversary.new DropAddressReplaceDuplicate(drop, duplicate));
                } else if (drop != 0) {
                    adversary.lie(adversary.new DropAddress(drop));
                }

                return adversary;
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

        public Map<SigningKey, ReturnState> run() {
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
    }

    public InitialState initialize(SessionIdentifier session, long amount) {
        return new InitialState(session, amount);
    }

    public Map<SigningKey, ReturnState> successfulRun(
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

    public Map<SigningKey, ReturnState> insufficientFundsRun(
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

    public Map<SigningKey, ReturnState> doubleSpendingRun(
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

    public Map<SigningKey, ReturnState> runWithReplacements(
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
                init.add(new Adversary(session, amount, key, players, coin, null).lie(maliciousPlayers.get(key)));
            } else {
                init.add(new Adversary(session, amount, key, players, coin, null));
            }
        }

        return runSimulation(init);
    }
}
