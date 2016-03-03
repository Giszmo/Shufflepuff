package com.shuffle.sim;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.CoinShuffle;
import com.shuffle.protocol.Machine;
import com.shuffle.protocol.MaliciousMachine;
import com.shuffle.protocol.MessageFactory;
import com.shuffle.protocol.Network;
import com.shuffle.protocol.SessionIdentifier;
import com.shuffle.protocol.blame.Evidence;
import com.shuffle.protocol.blame.Matrix;
import com.shuffle.protocol.blame.Reason;

import java.util.HashMap;
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
    // A blame matrix that matches any matrix given to it.
    // Used for ensuring a test can't fail no matter what value
    // simulated adversaries return, since we only care about testing the response of the
    // honest players.
    public static class MatrixPatternAny extends Matrix {

        @Override
        public boolean match(Matrix bm) {
            return true;
        }

        @Override
        public String toString() {
            return "Any";
        }
    }

    private final static class BlameEvidencePatternAny extends Evidence {
        private BlameEvidencePatternAny() {
            super(Reason.NoFundsAtAll, false, null, null, null, null, null);

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

    static public MatrixPatternAny anyMatrix = new MatrixPatternAny();
    static public BlameEvidencePatternAny anyReason = new BlameEvidencePatternAny();

    public final SessionIdentifier session;
    public final long amount;
    private final LinkedList<PlayerInitialState> players = new LinkedList<>();
    private MockCoin mockCoin = null;

    public List<PlayerInitialState> getPlayers() {
        List<PlayerInitialState> p = new LinkedList<>();
        p.addAll(players);
        return p;
    }

    public class PlayerInitialState {
        final SigningKey sk;
        final VerificationKey vk;
        SortedSet<VerificationKey> keys = new TreeSet<>();
        long initialAmount = 0;
        long spend = 0;
        long doubleSpend = 0;
        boolean change = false; // Whether to make a change address.
        int viewpoint = 1;

        // Whether the adversary should equivocate during the announcement phase and to whom.
        int[] equivocateAnnouncement = new int[]{};

        // Whether the adversary should equivocate during the broadcast phase and to whom.
        int[] equivocateOutputVector = new int[]{};

        int drop = 0; // Whether to drop an address in phase 2.
        int duplicate = 0; // Whether to duplicate another address and replace it with the dropped address.
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

        public MockCoin coin(Crypto crypto) {
            MockCoin coin = networkPoints.get(viewpoint);

            if (coin != null) {
                return coin;
            }

            if (mockCoin == null) {

                mockCoin = new com.shuffle.mock.MockCoin();

                for (PlayerInitialState player : players) {
                    if (player.initialAmount > 0) {
                        Address address = player.sk.VerificationKey().address();

                        Address previousAddress = crypto.makeSigningKey().VerificationKey().address();

                        mockCoin.put(previousAddress, player.initialAmount);
                        mockCoin.spend(previousAddress, address, player.initialAmount).send();

                        // Plot twist! We spend it all!
                        if (player.spend > 0) {
                            mockCoin.spend(address, crypto.makeSigningKey().VerificationKey().address(), player.spend).send();
                        }
                    }
                }
            }

            networkPoints.put(viewpoint, mockCoin.copy());
            return mockCoin;
        }

        public Adversary adversary(Crypto crypto, MessageFactory messages, Network network) {
            if (sk == null) {
                return null;
            }

            Address address = sk.VerificationKey().address();
            Transaction doubleSpendTrans = null;

            MockCoin coin = coin(crypto);

            if (doubleSpend > 0) {
                // is he going to double spend? If so, make a new transaction for him.
                doubleSpendTrans = coin.spend(address, crypto.makeSigningKey().VerificationKey().address(), doubleSpend);
            }

            CoinShuffle shuffle;

            if (equivocateAnnouncement != null && equivocateAnnouncement.length > 0) {
                shuffle = MaliciousMachine.announcementEquivocator(messages, crypto, coin, fromSet(keys, equivocateAnnouncement));
            } else if (equivocateOutputVector != null && equivocateOutputVector.length > 0) {
                shuffle = MaliciousMachine.broadcastEquivocator(messages, crypto, coin, fromSet(keys, equivocateOutputVector));
            } else if (replace && drop != 0) {
                shuffle = MaliciousMachine.addressDropperReplacer(messages, crypto, coin, drop);
            } else if (duplicate != 0 && drop != 0) {
                shuffle = MaliciousMachine.addressDropperDuplicator(messages, crypto, coin, drop, duplicate);
            } else if (drop != 0) {
                shuffle = MaliciousMachine.addressDropper(messages, crypto, coin, drop);
            } else {
                shuffle = new CoinShuffle(messages, crypto, coin);
            }

            return new Adversary(session, amount, sk, keys, shuffle, network);
        }

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
                    return Reason.ShuffleAndEquivocationFailure;
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

            return null;
        }

        public Matrix expectedBlame() {
            // Malicious players aren't tested, so they can blame anyone.
            if (maliciousBehavior() != null) {
                return anyMatrix;
            }

            Matrix bm = new Matrix();

            for(PlayerInitialState i : players) {
                for (PlayerInitialState j : players) {
                    // We don't care who malicious players blame because they aren't trustworthy anyway.
                    if (i.maliciousBehavior() != null) {
                        bm.put(i.vk, j.vk, anyReason);
                        continue;
                    }

                    // Don't let players blame themselves!
                    if (i.equals(j)) {
                        continue;
                    }

                    Reason reason = j.maliciousBehavior();

                    if (reason != null) {
                        if (equals(i) || reason == Reason.NoFundsAtAll || reason == Reason.InsufficientFunds) {
                            bm.put(i.vk, j.vk, Evidence.Expected(reason, true));
                        }
                    }
                }
            }

            return bm;
        }
    }

    public Map<Integer, MockCoin> networkPoints = new HashMap<>();

    public InitialState(SessionIdentifier session, long amount) {

        this.session = session;
        this.amount = amount;
    }

    public InitialState player(SigningKey key) {

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

    public Map<SigningKey, Matrix> expectedBlame() {
        Map<SigningKey, Matrix> blame = new HashMap<>();

        for (PlayerInitialState player : players) {
            if (player.sk != null) {
                blame.put(player.sk, player.expectedBlame());
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

    public static InitialState successful(
            SessionIdentifier session,
            int numPlayers,
            long amount,
            Crypto crypto
    ) {
        InitialState init = new InitialState(session, amount);

        for (int i = 1; i <= numPlayers; i++) {
            init.player(crypto.makeSigningKey()).initialFunds(20);
        }

        return init;
    }

    public static InitialState insufficientFunds(
            SessionIdentifier session,
            int numPlayers,
            int[] deadbeats, // Players who put no money in their address.
            int[] poor, // Players who didn't put enough in their address.
            int[] spenders, // Players who don't have enough because they spent it.
            long amount,
            Crypto crypto
    ) {
        InitialState init = new InitialState(session, amount);

        pit : for (int i = 1; i <= numPlayers; i++) {
            init.player(crypto.makeSigningKey()).initialFunds(20);
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
}
