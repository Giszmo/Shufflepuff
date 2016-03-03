package com.shuffle.player;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Coin;
import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.p2p.Channel;
import com.shuffle.protocol.CoinShuffle;
import com.shuffle.protocol.Machine;
import com.shuffle.protocol.Mailbox;
import com.shuffle.protocol.MessageFactory;
import com.shuffle.protocol.Phase;
import com.shuffle.protocol.SessionIdentifier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 *
 * Created by Daniel Krawisz on 2/1/16.
 */
public class Player<Identity, Format> {
    private final static Logger log= LogManager.getLogger(Player.class);

    private final SigningKey sk;

    private final Crypto crypto;

    private final Coin coin;

    private final MessageFactory messages;

    public class Settings {
        final SessionIdentifier session;
        final long amount;
        final Address change;
        final int minPlayers;
        final int maxRetries;
        final int timeout;

        public Settings(SessionIdentifier session, long amount, Address change, int minPlayers, int maxRetries, int timeout) {
            this.session = session;
            this.amount = amount;
            this.change = change;
            this.minPlayers = minPlayers;
            this.maxRetries = maxRetries;
            this.timeout = timeout;
        }
    }

    public Player(
            SigningKey sk,
            MessageFactory messages, // Object that knows how to create and copy messages.
            Crypto crypto, // Connects to the cryptography.
            Coin coin // Connects us to the BitcoinCrypto or other cryptocurrency netork.
    ) {
        if (sk == null || crypto == null || coin == null || messages == null) {
            throw new NullPointerException();
        }
        this.sk = sk;
        this.crypto = crypto;
        this.coin = coin;
        this.messages = messages;
    }

    public List<Machine> coinShuffle(
            Set<Identity> identities,
            Channel<Identity, Format> channel,
            Marshaller<Format> marshaller,
            Map<Identity, VerificationKey> keys, // Can be null.
            Settings settings,
            LinkedBlockingQueue<Machine> queue
    ) {
        SessionIdentifier session = settings.session;
        Network<Identity, Format> net = new Network<>(channel, marshaller, settings.timeout);

        // Start by making connections to all the identies.
        for (Identity identity : identities) {
            channel.getPeer(identity);
        }

        SortedSet<VerificationKey> players = new TreeSet<>();
        players.add(sk.VerificationKey());
        // if the keys have not been given, send out those.
        if (keys == null) {
            // TODO
            keys = new HashMap<>();
        }

        for (VerificationKey key : keys.values()) {
            players.add(key);
        }

        // Try the protocol.
        List<Machine> list = new LinkedList<>();
        int attempt = 0;

        // The eliminated players. A player is eliminated when there is a subset of players
        // which all blame him and none of whom blame one another.
        SortedSet<VerificationKey> eliminated = new TreeSet<>();

        CoinShuffle shuffle = new CoinShuffle(messages, crypto, coin);

        while (true) {

            if (players.size() - eliminated.size() < settings.minPlayers) {
                break;
            }

            // Get the initial ordering of the players.
            int i = 1;
            SortedSet<VerificationKey> validPlayers = new TreeSet<>();
            for (VerificationKey player : players) {
                if (!eliminated.contains(player)) {
                    validPlayers.add(player);
                    i++;
                }
            }

            // Make an inbox for the next round.
            Mailbox mailbox = new Mailbox(session, sk, validPlayers, net);

            // Send an introductory message and make sure all players agree on who is in
            // this round of the protocol.
            // TODO

            Machine machine = shuffle.runProtocol(session, settings.amount, sk, validPlayers, settings.change, net, queue);

            if (machine.exception() == null && machine.phase() != Phase.Blame) {
                break;
            }

            attempt++;

            if (attempt > settings.maxRetries) {
                break;
            }

            break; // TODO remove this line.
            // TODO
            // Determine if the protocol can be restarted with some players eliminated.

            // First, if a player had insufficient funds or not enough funds, does everyone
            // else agree that this player needs to be eliminated? If so, eliminate that player.

            // If there was an equivocation check failure, does everyone agree as to the player
            // who caused it? If so then we can restart.

            // If there was a shuffle failure, does everyone agree as to the accused? If so then
            // eliminate that player.

            // If we can restart, broadcast a message to that effect and wait to receive a
            // similar message from the remaining players.

        }

        return list;
    }
}
