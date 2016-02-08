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
import com.shuffle.protocol.SessionIdentifier;

import java.util.Deque;
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
    private final SessionIdentifier session;
    private final long amount;
    private final Deque<PlayerInitialState> players = new LinkedList<>();
    private MockCoin defaultCoin = null;
    private List<MockCoin> coins;

    private class PlayerInitialState {
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

        PlayerInitialState() {
        }

        Adversary adversary(Map<PlayerInitialState, SigningKey> keys, Crypto crypto, MessageFactory messages, Network network) {
            MockCoin newCoin;
            if (coin != null) {
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
            Machine machine = new Machine(session, amount, key, identities);

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

            return new Adversary(session, amount, key, identities, newCoin, doubleSpendTrans, shuffle, machine, network);
        }
    }

    public InitialState(SessionIdentifier session, long amount) {

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
        players.addLast(new PlayerInitialState());
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

    public InitialState doubleSpend(long amount) {
        players.getLast().doubleSpend = amount;
        return this;
    }

    public InitialState coin(MockCoin coin) {
        players.getLast().coin = coin;
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

    public Map<SigningKey, Machine> run(Simulator simulator) {
        List<Adversary> adversaries = new LinkedList<>();
        Map<PlayerInitialState, SigningKey> keys = new HashMap<>();

        for (PlayerInitialState player : players) {
            keys.put(player, simulator.crypto.makeSigningKey());
        }

        // Check that all players have a coin network set up, either the default or their own.
        for (PlayerInitialState player : players) {
            Adversary adversary = player.adversary(keys, simulator.crypto, simulator.messages, new Network(simulator));
            if (adversary == null) {
                return null;
            }
            adversaries.add(adversary);
        }

        return simulator.runSimulation(adversaries);
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
}
