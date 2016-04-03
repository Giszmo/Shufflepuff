/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.sim;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.CoinNetworkException;
import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.CoinShuffle;
import com.shuffle.protocol.MaliciousMachine;
import com.shuffle.protocol.MessageFactory;
import com.shuffle.protocol.Network;
import com.shuffle.protocol.SessionIdentifier;
import com.shuffle.protocol.blame.Evidence;
import com.shuffle.protocol.blame.Matrix;
import com.shuffle.protocol.blame.Reason;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A representation of an initial state for a protocol. Can specify various kinds of
 * malicious behavior.
 *
 * Created by Simulator on 2/8/16.
 */
public class InitialState {
    // An expected return state that matches any blame matrix, even a null one.
    // Used for ensuring a test can't fail no matter what value
    // simulated adversaries return, since we only care about testing the response of the
    // honest players.
    public static final class ExpectedPatternAny extends Matrix {

        public ExpectedPatternAny() {

        }

        @Override
        public boolean match(Matrix m) {
            return true;
        }

        @Override
        public String toString() {
            return "Any";
        }
    }


    // An expected return state that matches any null blame matrix.
    public static final class ExpectedPatternNull extends Matrix {

        public ExpectedPatternNull() {

        }

        @Override
        public boolean match(Matrix m) {
            return m == null;
        }

        @Override
        public String toString() {
            return "Null";
        }
    }

    private static final class EvidencePatternAny extends Evidence {
        private EvidencePatternAny(VerificationKey accused) {
            super(accused, Reason.NoFundsAtAll, null, null, null, null, null, null, null);

        }

        @Override
        public boolean match(Evidence e) {
            return true;
        }

        @Override
        public String toString() {
            return "Any";
        }
    }

    private static final class EvidencePatternOr extends Evidence {
        private final Evidence or;

        EvidencePatternOr(VerificationKey accused, Reason reason, Evidence or) {
            super(accused, reason);
            this.or = or;
        }

        @Override
        public boolean match(Evidence e) {

            if (super.match(e)) {
                return true;
            }

            if (or == null) {
                return e == null;
            }

            return or.match(e);
        }

        @Override
        public String toString() {
            return super.toString() + "|" + or;
        }
    }

    private static final ExpectedPatternAny anyMatrix = new ExpectedPatternAny();
    private static final ExpectedPatternNull nullMatrix = new ExpectedPatternNull();

    private final SessionIdentifier session;
    private final long amount;
    private final Crypto crypto;
    private final LinkedList<PlayerInitialState> players = new LinkedList<>();
    private MockCoin mockCoin = null;

    // The initial state of an individual player. (includes malicious players)
    public final class PlayerInitialState {
        final SigningKey sk;
        final VerificationKey vk;
        final SortedSet<VerificationKey> keys = new TreeSet<>();
        long initialAmount = 0;
        long spend = 0;
        long doubleSpend = 0;
        boolean change = false; // Whether to make a change address.
        int viewpoint = 1;
        boolean mutate = false; // Whether to create a mutated transaction.

        // Whether the adversary should equivocate during the announcement phase and to whom.
        int[] equivocateAnnouncement = new int[]{};

        // Whether the adversary should equivocate during the broadcast phase and to whom.
        int[] equivocateOutputVector = new int[]{};

        int drop = 0; // Whether to drop an address in phase 2.
        int duplicate = 0; // Whether to duplicate another address to replace it with.
        boolean replace = false; // Whether to replace dropped address with a new one.

        PlayerInitialState(SigningKey sk) {
            this.sk = sk;
            vk = sk.VerificationKey();
        }

        PlayerInitialState(VerificationKey vk) {
            this.sk = null;
            this.vk = vk;
        }

        public SessionIdentifier getSession() {
            return InitialState.this.session;
        }

        public long getAmount() {
            return InitialState.this.amount;
        }

        public Crypto crypto() {
            return InitialState.this.crypto;
        }

        public MockCoin coin() throws CoinNetworkException {
            if (networkPoints == null) {
                networkPoints = new HashMap<>();
            }

            MockCoin coin = networkPoints.get(viewpoint);

            if (coin != null) {
                return coin;
            }

            if (mockCoin == null) {

                mockCoin = new com.shuffle.mock.MockCoin();

                for (PlayerInitialState player : players) {
                    if (player.initialAmount > 0) {
                        Address address = player.sk.VerificationKey().address();

                        Address previousAddress
                                = crypto.makeSigningKey().VerificationKey().address();

                        mockCoin.put(previousAddress, player.initialAmount);
                        mockCoin.makeSpendingTransaction(
                                previousAddress, address, player.initialAmount
                        ).send();

                        // Plot twist! We spend it all!
                        if (player.spend > 0) {
                            mockCoin.makeSpendingTransaction(
                                    address,
                                    crypto.makeSigningKey().VerificationKey().address(),
                                    player.spend
                            ).send();
                        }
                    }
                }
            }

            MockCoin copy = mockCoin.copy();
            networkPoints.put(viewpoint, copy);
            return copy;
        }

        // Turn the initial state into an Adversary object that can be run in the simulator.
        public Adversary adversary(
                MessageFactory messages,
                Network network
        ) throws CoinNetworkException {

            if (sk == null) {
                return null;
            }

            Address address = sk.VerificationKey().address();
            MockCoin coin = coin();
            CoinShuffle shuffle;

            if (equivocateAnnouncement != null && equivocateAnnouncement.length > 0) {
                shuffle = MaliciousMachine.announcementEquivocator(
                        messages, crypto, coin, fromSet(keys, equivocateAnnouncement)
                );
            } else if (equivocateOutputVector != null && equivocateOutputVector.length > 0) {
                shuffle = MaliciousMachine.broadcastEquivocator(
                        messages, crypto, coin, fromSet(keys, equivocateOutputVector)
                );
            } else if (replace && drop != 0) {
                shuffle = MaliciousMachine.addressReplacer(messages, crypto, coin, drop);
            } else if (duplicate != 0 && drop != 0) {
                shuffle = MaliciousMachine.addressDropperDuplicator(
                        messages, crypto, coin, drop, duplicate
                );
            } else if (drop != 0) {
                shuffle = MaliciousMachine.addressDropper(messages, crypto, coin, drop);
            } else if (doubleSpend > 0) {
                // is he going to double spend? If so, make a new transaction for him.
                shuffle = MaliciousMachine.doubleSpender(messages, crypto, coin,
                        coin.makeSpendingTransaction(
                                address,
                                crypto.makeSigningKey().VerificationKey().address(),
                                doubleSpend
                        )
                );
            } else if (mutate) {
                shuffle = new CoinShuffle(messages, crypto, coin.mutated());
            } else {
                shuffle = new CoinShuffle(messages, crypto, coin);
            }

            return new Adversary(session, amount, sk, keys, shuffle, network);
        }

        // The sort of malicious behavior to be performed by this player, if any.
        public Reason maliciousBehavior() {
            // Does the player have enough funds?
            if (initialAmount == 0) {
                return Reason.NoFundsAtAll;
            }

            if (initialAmount < amount || initialAmount - spend < amount) {
                return Reason.InsufficientFunds;
            }

            // Is the player going to equivocate in phase 1?
            if (equivocateAnnouncement != null && equivocateAnnouncement.length > 0) {
                return Reason.EquivocationFailure;
            }

            // Is the player going to drop an address during the shuffle phase?
            if (drop != 0) {
                if (replace) {
                    return Reason.ShuffleFailure;
                }
                return Reason.ShuffleFailure;
            }

            // Is the player going to equivocate in phase 3?
            if (equivocateOutputVector != null && equivocateOutputVector.length > 0) {
                return Reason.EquivocationFailure;
            }

            // Is the player going to double spend?
            if (doubleSpend > 0) {
                return Reason.DoubleSpend;
            }

            // Is the player going to produce the wrong transaction?
            if (mutate) {
                return Reason.InvalidSignature;
            }

            return null;
        }

        // How is the player expected to interpret what happened during the protocol?
        public Matrix expected() {
            // Malicious players aren't tested, so they can blame anyone.
            Reason mal = maliciousBehavior();
            if (mal != null) {
                return anyMatrix;
            }

            Matrix bm = new Matrix();

            for (PlayerInitialState i : players) {
                for (PlayerInitialState j : players) {
                    // We don't care who malicious players blame.
                    if (i.maliciousBehavior() != null) {
                        bm.put(i.vk, new EvidencePatternAny(j.vk));
                        continue;
                    }

                    // Don't let players blame themselves!
                    if (i.equals(j)) {
                        continue;
                    }

                    Reason reason = j.maliciousBehavior();

                    if (reason == null) {
                        continue;
                    }

                    if (reason == Reason.DoubleSpend) {
                        if (i.viewpoint == j.viewpoint) {
                            bm.put(i.vk, Evidence.Expected(j.vk, reason));
                        } else {
                            bm.put(i.vk, new EvidencePatternOr(j.vk, reason, null));
                        }
                        continue;
                    }

                    if (reason == Reason.NoFundsAtAll || reason == Reason.InsufficientFunds
                            || reason == Reason.InvalidSignature) {

                        bm.put(i.vk, Evidence.Expected(j.vk, reason));
                        continue;
                    }

                    if (equals(i)) {
                        bm.put(i.vk, Evidence.Expected(j.vk, reason));
                    }
                }
            }

            return (bm.isEmpty() ? nullMatrix : bm);
        }
    }

    public interface Initializer {
        MessageFactory messages(VerificationKey key);

        Network network(VerificationKey key);
    }

    public Map<SigningKey, Adversary> getPlayers(Initializer initializer) {
        Map<SigningKey, Adversary> p = new HashMap<>();

        for (PlayerInitialState player : players) {
            if (player.sk == null) {
                continue;
            }

            try {
                p.put(player.sk,
                        player.adversary(
                                initializer.messages(player.vk),
                                initializer.network(player.vk)
                        ));

            } catch (CoinNetworkException e) {
                return null; // Should not really happen.
            }
        }

        networkPoints = null;

        return p;
    }

    public List<VerificationKey> getKeys() {
        List<VerificationKey> keys = new LinkedList<>();

        for (PlayerInitialState player : players) {
            keys.add(player.vk);
        }

        return keys;
    }

    public PlayerInitialState getPlayer(int n) {
        return players.get(n);
    }

    private Map<Integer, MockCoin> networkPoints = null;

    public InitialState(SessionIdentifier session, long amount, Crypto crypto) {

        this.session = session;
        this.amount = amount;
        this.crypto = crypto;
    }

    public InitialState player() {
        SigningKey key = crypto.makeSigningKey();

        PlayerInitialState next = new PlayerInitialState(key);
        PlayerInitialState last = players.peekLast();
        if (last != null) {
            next.keys.addAll(last.keys);
        }
        players.addLast(next);

        for (PlayerInitialState player : players) {
            player.keys.add(key.VerificationKey());
        }
        return this;
    }

    public InitialState initialFunds(long amount) {
        players.getLast().initialAmount = amount;
        return this;
    }

    public InitialState spend(long amount) {
        players.getLast().spend = amount;
        return this;
    }

    public InitialState networkPoint(int i) {
        players.getLast().viewpoint = i;
        return this;
    }

    public InitialState doubleSpend(long amount) {
        players.getLast().doubleSpend = amount;
        return this;
    }

    public InitialState equivocateAnnouncement(int[] equivocate) {
        players.getLast().equivocateAnnouncement = equivocate;
        return this;
    }

    public InitialState equivocateOutputVector(int[] equivocate) {
        players.getLast().equivocateOutputVector = equivocate;
        return this;
    }

    public InitialState change() {
        players.getLast().change = true;
        return this;
    }

    public InitialState drop(int drop) {
        players.getLast().drop = drop;
        players.getLast().duplicate = 0;
        players.getLast().replace = false;
        return this;
    }

    public InitialState replace(int drop, int duplicate) {
        players.getLast().drop = drop;
        players.getLast().duplicate = duplicate;
        players.getLast().replace = false;
        return this;
    }

    public InitialState replace(int drop) {
        players.getLast().drop = drop;
        players.getLast().duplicate = 0;
        players.getLast().replace = true;
        return this;
    }

    public InitialState mutateTransaction() {
        players.getLast().mutate = true;
        return this;
    }

    public Map<SigningKey, Matrix> expected() {
        Map<SigningKey, Matrix> blame = new HashMap<>();

        for (PlayerInitialState player : players) {
            if (player.sk != null) {
                blame.put(player.sk, player.expected());
            }
        }

        return blame;
    }

    private Set<VerificationKey> fromSet(SortedSet<VerificationKey> identities, int[] array) {
        Set<VerificationKey> others = new TreeSet<>();

        int p = 1;
        int i = 0;
        for (VerificationKey player : identities) {
            while (i < array.length && array[i] < p) {
                i++;
            }

            if (i < array.length && array[i] == p) {
                others.add(player);
            }

            p++;
        }

        return others;
    }

    // An initial state containing no malicious players.
    public static InitialState successful(
            final SessionIdentifier session,
            final long amount,
            final Crypto crypto,
            final int numPlayers
    ) {
        InitialState init = new InitialState(session, amount, crypto);

        for (int i = 1; i <= numPlayers; i++) {
            init.player().initialFunds(20);
        }

        return init;
    }

    // Initial state for cases in which players cannot afford to engage in the round, for
    // one reason or another.
    public static InitialState insufficientFunds(
            final SessionIdentifier session,
            final long amount,
            final Crypto crypto,
            final int numPlayers,
            final int[] deadbeats, // Players who put no money in their address.
            final int[] poor, // Players who didn't put enough in their address.
            final int[] spenders // Players who don't have enough because they spent it.
    ) {
        InitialState init = new InitialState(session, amount, crypto);

        pit : for (int i = 1; i <= numPlayers; i++) {
            init.player().initialFunds(20);
            for (int deadbeat : deadbeats) {
                if (deadbeat == i) {
                    init.initialFunds(0);
                    continue pit;
                }
            }
            for (int aPoor : poor) {
                if (aPoor == i) {
                    init.initialFunds(10);
                    continue pit;
                }
            }
            for (int spender : spenders) {
                if (spender == i) {
                    init.spend(16);
                }
            }
        }

        return init;
    }

    // Initial state for players who spend their funds while the protocol is running.
    public static InitialState doubleSpend(
            final SessionIdentifier session,
            final long amount,
            final Crypto crypto,
            final int[] views, // Each player may have a different view of the network; ie,
                              // some players may be able to observe that the double spend has
                             // occurred but others may not.
            final int[] spenders  // The set of players who attempt to double spend.
    ) {
        final Set<Integer> doubleSpenders = new HashSet<>();

        for (int d : spenders) {
            doubleSpenders.add(d);
        }

        InitialState init = new InitialState(session, amount, crypto);
        for (int i = 0; i < views.length; i ++) {
            init.player().initialFunds(20).networkPoint(views[i]);

            if (doubleSpenders.contains(i + 1)) {
                init.doubleSpend(13);
            }
        }
        return init;
    }

    // A class used to define certain kinds of initial states.
    public static class Equivocation {
        final int equivocator;
        final int[] equivocation;

        public Equivocation(int equivocator, int[] equivocation) {
            // Testing the case where the first player is the equivocator is too hard for now.
            // It would require basically writing a whole new version of protocolDefinition()
            // to be a test function. It is unlikely that testing case will find a bug in the code.
            if (equivocator == 1) {
                throw new IllegalArgumentException();
            }

            for (int eq : equivocation) {
                if (eq <= equivocator) {
                    throw new IllegalArgumentException();
                }
            }

            this.equivocator = equivocator;
            this.equivocation = equivocation;
        }

        @Override
        public String toString() {
            return "equivocation[" + equivocator + ", " + Arrays.toString(equivocation) + "]";
        }
    }

    // Initial state for malicious players who equivocate during the announcement phase.
    public static InitialState equivocateAnnouncement(
            final SessionIdentifier session,
            final long amount,
            final Crypto crypto,
            final int numPlayers,
            final Equivocation[] equivocators
    ) {
        InitialState init = new InitialState(session, amount, crypto);

        int eq = 0;
        for (int i = 1; i <= numPlayers; i ++) {
            init.player().initialFunds(20);

            while (eq < equivocators.length && equivocators[eq].equivocator < i) {
                eq++;
            }

            if (eq < equivocators.length && equivocators[eq].equivocator == i) {
                init.equivocateAnnouncement(equivocators[eq].equivocation);
            }
        }

        return init;
    }

    // Initial state for a player who equivocates during the broadcast phase.
    public static InitialState equivocateBroadcast(
            final SessionIdentifier session,
            final long amount,
            final Crypto crypto,
            final int numPlayers,
            final int[] equivocation
    ) {

        InitialState init = new InitialState(session, amount, crypto);

        // Only the last player can equivocate.
        for (int i = 1; i < numPlayers; i ++) {
            init.player().initialFunds(20);
        }

        // Add the malicious equivocator.
        init.player().initialFunds(20).equivocateOutputVector(equivocation);

        return init;
    }

    // Initial state for players who shuffle their addresses incorrectly.
    public static InitialState dropAddress(
            final SessionIdentifier session,
            final long amount,
            final Crypto crypto,
            final int numPlayers,
            final int[][] drop,
            final int[][] replaceNew,
            final int[][] replaceDuplicate
    ) {

        final Map<Integer, Integer> dropMap = new HashMap<>();
        final Map<Integer, Integer> replaceNewMap = new HashMap<>();
        final Map<Integer, Integer[]> replaceDuplicateMap = new HashMap<>();


        if (drop != null) {
            for (int[] d : drop) {
                if (d.length == 2 && d[1] < d[0]) {
                    dropMap.put(d[0], d[1]);
                }
            }
        }

        if (replaceDuplicate != null) {
            for (int[] d : replaceDuplicate) {
                if (d.length == 2 && d[1] < d[0]) {
                    replaceDuplicateMap.put(d[0], new Integer[]{d[1], d[2]});
                }
            }
        }

        if (replaceNew != null) {
            for (int[] d : replaceNew) {
                if (d.length == 2 && d[1] < d[0]) {
                    replaceNewMap.put(d[0], d[1]);
                }
            }
        }


        InitialState init = new InitialState(session, amount, crypto);

        for (int i = 1; i <= numPlayers; i ++) {

            init.player().initialFunds(20);

            if (dropMap.containsKey(i)) {
                init.drop(dropMap.get(i));
            } else if (replaceDuplicateMap.containsKey(i)) {
                Integer[] dup = replaceDuplicateMap.get(i);
                init.replace(dup[0], dup[1]);
            } else if (replaceNewMap.containsKey(i)) {
                init.replace(replaceNewMap.get(i));
            }
        }

        return init;
    }

    public static InitialState invalidSignature(
            final SessionIdentifier session,
            final long amount,
            final Crypto crypto,
            final int numPlayers,
            final int[] mutants) {
        final Set<Integer> mutantsSet = new HashSet<>();

        for (int mutant : mutants) {
            mutantsSet.add(mutant);
        }

        InitialState init = new InitialState(session, amount, crypto);

        for (int i = 1; i <= numPlayers; i ++) {
            init.player().initialFunds(20);

            if (mutantsSet.contains(i)) {
                init.mutateTransaction();
            }
        }

        return init;
    }
}
