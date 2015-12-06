package com.shuffle.form;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * Abstract implementation of CoinShuffle in java.
 * http://crypsys.mmci.uni-saarland.de/projects/CoinShuffle/coinshuffle.pdf
 *
 * The ShuffleMachine is the abstract state machine that defines the CoinShuffle protocol. All
 * protocols ultimately are definitions of abstract state machines, so that is what this class is.
 *
 * This class was designed so that someone with experience in cryptography and Bitcoin should be
 * able to understand the CoinShuffle protocol simply by reading its code, without necessarily
 * having to refer back to the original paper. It follows the original paper as closely as possible
 * and therefore assumes as much about the underlying primitives that it refers to as would a
 * paper on cryptography attempting to best exposit the protocol.
 *
 * In particular, it assumes that all cryptographic operations, network interactions, and queries
 * upon the Bitcoin network are correct as far as is required of a succinct description of the
 * protocol. Therefore, this class can only be used correctly by someone capable of reading the
 * original paper and discerning the relevent security criteria for each primitive operation.
 *
 *
 * Created by Daniel Krawisz on 12/3/15.
 *
 */
public class ShuffleMachine {

    VerificationKey players[]; // The participants themselves.

    int N; // the number of participants.

    SigningKey sk;

    // What is my index among the participants? (the protocol as described in the paper uses
    // ordinal numbering, so the value of me is actually one greater than the index of our key
    // in the array.)
    int me;

    Network network;

    Crypto crypto;

    Coin coin;

    MessageFactory message;

    volatile ShufflePhase phase;

    public ShuffleMachine(
            SessionIdentifier τ, // The session key for this run of the protocol.
            VerificationKey players[], // The keys representing all the players.
            MessageFactory message,
            Crypto crypto,
            Coin coin,
            Network network) {

        this.players = players;
        this.N = players.length;
        this.network = network;
        this.crypto = crypto;
        this.coin = coin;

        this.phase = ShufflePhase.Uninitiated;

        // Register the relevant information with the message generator.
        message.register(τ, sk, this);

        // These are the keys from which we will receive messages.
        network.keys(opponentSet(1, N));
    }

    // the phase can be accessed concurrently in case we want to update
    // the user on how the protocol is going.
    public ShufflePhase currentPhase() {
        return phase;
    }

    public void run(
            CoinAmount ν, // The amount to be shuffled per particpant.
            SigningKey sk // The private key that represents us!
    ) throws
                SessionIdentifierException,
                ProtocolStartedException,
                ProtocolAbortedException,
                TimeoutException,
                FormatException,
                CryptographyException,
                BlockChainException,
                MempoolException,
                InvalidImplementationException {

        // Don't let the protocol be run more than once at a time.
        if (phase != ShufflePhase.Uninitiated) {
            throw new ProtocolStartedException();
        }

        // Determine what my index number is.
        for (int i = 1; i <= N; i ++) {
            if (players[i - 1].equals(sk.VerificationKey())) {
                me = i;
            }
        }

        // The protocol is surrounded by a try block that catches BlameExceptions, which are thrown
        // when any other player deliberately breaks the rules.
        try {

        // Phase 0: This phase is not defined in the original paper. Instead, it is described
        // in phase 1. However, there was a problem with the wording of the original paper which
        // would have caused the check I have done here to fail. We check here whether each
        // player has the required funds to perform the shuffle successfully.
        phase = ShufflePhase.Initiated;

            // Check that each participant has the required amounts.
            for(VerificationKey participant : players) {
                if (!coin.valueHeld(participant).greater(ν)) {
                    throw new BlameException();
                }
            }

        // Phase 1: Announcement
        // In the announcement phase, participants distribute temporary encryption keys.
        phase = ShufflePhase.Announcement;

            // This will contain the new public keys.
            Map<VerificationKey, Message> σ1 = new TreeMap<>();

            // Everyone except player 1 creates a new keypair and sends it around to everyone else.
            DecryptionKey dk = null;
            EncryptionKey ek;
            if (me != 1) {
                dk = crypto.DecryptionKey();
                ek = dk.EncryptionKey();

                // Broadcast the key and store it in the set with everyone else's.
                σ1.put(sk.VerificationKey(), message.make(ek));
                network.broadcast(message.make(ek));

            }

            // Now we wait to receive similar messages from everyone else.
            σ1.putAll(network.receive(opponentSet(2, N)));

        // Phase 2: Shuffle
        // In the shuffle phase, we create a sequence of orderings which will b successively
        // applied by each particpant. Everyone has the incentive to insert their own address
        // at a random location, which is sufficient to ensure randomness of the whole thing
        // to all participants.
        phase = ShufflePhase.Shuffling;

            // Each participant chooses a new bitcoin address which will be their new outputs.
            SigningKey sk_new = crypto.SigningKey();
            VerificationKey vk_new = sk_new.VerificationKey();

            // Player one begins the cycle and encrypts its new address with everyone's key, in order.
            // Each subsequent player reorders the cycle and removes one layer of encryption.
            Message σ2 = message.make();
            if (me != 1) {
                Message σ2_last = network.receive();
                assert dk != null;
                σ2 = message.remake(dk.Decrypt(σ2_last));
            }

            // Add our own address to the mix. Note that if me == N, ie, the last player, then no
            // encryption is done. That is because we have reached the last layer of encryption.
            Message encrypted = message.make(vk_new);
            for (int i = me; i < N; i++) {
                // Successively encrypt with the keys of the players who haven't had their turn yet.
                encrypted = σ1.get(players[i]).readAsEncryptionKey().encrypt(encrypted);
            }

            // Insert new entry and reorder the keys.
            σ2.append(encrypted);
            shuffle(σ2);

            // Pass it along to the next player.
            if (me != N) {
                network.sendTo(players[me], σ2);
            }

        // Phase 3: broadcast outputs.
        // In this phase, the last player just broadcasts the transaction to everyone else.
        phase = ShufflePhase.BroadcastOutput;

            Queue<VerificationKey> σ3;
            if (me == N) {
                σ3 = σ2.readAsVerificationKeyList();
                network.broadcast(message.remake(σ2));
            } else {
                σ3 = network.receive().readAsVerificationKeyList();
            }

            // Everyone else receives the broadcast and checks to make sure their message was included.
            if (!σ3.contains(vk_new)) {
                throw new BlameException();
            }

        // Phase 4: equivocation check.
        // In this phase, participants check whether any player has sent different
        // encryption keys to different players.
        phase = ShufflePhase.EquivocationCheck;

            // Put all temporary encryption keys into a list and hash the result.
            Message σ4 = message.make();
            for (int i = 1; i < N; i++) {
                // Successively encrypt with the keys of the players who haven't had their turn yet.
                σ4.append(message.make(σ1.get(players[i - 1]).readAsEncryptionKey()));
            }

            σ4 = crypto.hash(σ4);
            network.broadcast(σ4);

            // Wait for a similar message from everyone else and check that the result is the name.
            Map<VerificationKey, Message> hash = network.receive(opponentSet(1, N));
            hash.put(sk.VerificationKey(), σ4);
            if (!areEqual(hash)) {
                throw new BlameException();
            }

        // Phase 5: verification and submission.
        // Everyone creates a Bitcoin transaction and signs it, then broadcasts the signature.
        // If all signatures check out, then the transaction is sent into the network.
        phase = ShufflePhase.VerificationAndSubmission;

            List<VerificationKey> inputs = new LinkedList<>();
            LinkedHashMap<VerificationKey, CoinAmount> outputs = new LinkedHashMap<>();
            Collections.addAll(inputs, players);
            for(VerificationKey key : σ3) {
                outputs.put(key, ν);
            }
            CoinTransaction t = coin.transaction(inputs, outputs);
            network.broadcast(message.make(sk.sign(t)));

            Map<VerificationKey, Message> σ5 = network.receive(opponentSet(1, N));

            // Verify the signatures.
            for(Map.Entry<VerificationKey, Message> sig : σ5.entrySet()) {
                if (!sig.getKey().verify(t, sig.getValue().readAsSignature())) {
                    throw new BlameException();
                }
            }

            // Check that the transaction is still valid.
            for(VerificationKey input : inputs) {
                if (!coin.unspent(input)) {
                    throw new BlameException();
                }
            }

            // Send the transaction into the network.
            coin.send(t);

        // The protocol has completed successfully.
        phase = ShufflePhase.Completed;

        } catch (BlameException e) {
            phase = ShufflePhase.Blame;
            // TODO This is where we go if we detect malicious bahavior on the part of another player.
        }
    }

    // Get the set of players other than myself from i to N.
    Set<VerificationKey> opponentSet(int i, int N) {
        Set<VerificationKey> set = new TreeSet<>();
        for(int j = i; j <= N; j ++) {
            if (j != me) {
                set.add(players[i - 1]);
            }
        }

        return set;
    }

    // Algorithm to randomly shuffle a linked list.
    void shuffle(Message σ) throws CryptographyException, InvalidImplementationException, FormatException {
        Message temp = message.make();
        int N = σ.size();

        // First remove all the elements and put them in the temp list.
        while (σ.size() > 0) {
            temp.append(σ.remove());
        }

        // Then successively and randomly select which one will be inserted until none remain.
        for (int i = N; i > 0; i --) {
            // Get a random number between 0 and N - 1 inclusive.
            int n = crypto.getRandom(N - 1);

            for (int j = 0; j < n; j ++) {
                temp.append(temp.remove());
            }

            // add the randomly selected element to the queue.
            σ.append(temp.remove());
        }
    }

    boolean areEqual(Map<VerificationKey, Message> messages) {
        boolean equal = true;

        Message last = null;
        for (Map.Entry<VerificationKey, Message> e : messages.entrySet()) {
            if (last != null) {
                equal = equal&&last.equal(e.getValue());
                if (!equal) {
                    return false;
                }
            }

            last = e.getValue();
        }

        return equal;
    }
}
