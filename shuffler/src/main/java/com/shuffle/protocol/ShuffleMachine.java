package com.shuffle.protocol;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

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
public final class ShuffleMachine {
    SessionIdentifier τ;

    int me;

    Crypto crypto;

    Coin coin;

    MessageFactory messages;

    Network connection;

    NetworkOperations network;

    ShufflePhase phase;

    // A class that is returned if a player is identified as having attempted to cheat.
    public final class Blame {
        ShufflePhase phase;
        int cheater;

        @Override
        public boolean equals(Object o) {
            if(!(o instanceof ShuffleMachine.Blame)) {
                return false;
            }

            Blame b = (Blame)o;

            return phase == b.phase && cheater == b.cheater;
        }

        @Override
        public int hashCode() {
            return 13 * cheater + 17 * phase.ordinal();
        }
    }

    Set<Blame> blame = new HashSet<>();

    public ShuffleMachine(
            SessionIdentifier τ,
            MessageFactory messages,
            Crypto crypto,
            Coin coin,
            Network connection) {

        this.τ = τ;
        this.crypto = crypto;
        this.coin = coin;
        this.messages = messages;
        this.connection = connection;

        this.phase = ShufflePhase.Uninitiated;
    }

    void protocolDefinition(
            CoinAmount ν, // The amount to be shuffled.
            SigningKey sk, // My signing key.
            VerificationKey players[] // The keys representing all the players, in order.
    ) throws
            TimeoutException,
            FormatException,
            CryptographyException,
            BlockchainException,
            MempoolException,
            InvalidImplementationException,
            ValueException,
            CoinNetworkException,
            InvalidParticipantSetException,
            InterruptedException {

        me = -1;
        int N = players.length;
        VerificationKey pk = sk.VerificationKey();

        // Determine what my index number is.
        for (int i = 1; i <= N; i ++) {
            if (players[i - 1].equals(pk)) {
                me = i;
            }
        }

        if (me < 0) {
            throw new InvalidParticipantSetException();
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
            for(VerificationKey player : players) {
                if (!coin.valueHeld(player).greater(ν)) {
                    throw new BlameException();
                }
            }

        // Phase 1: Announcement
        // In the announcement phase, participants distribute temporary encryption keys.
        phase = ShufflePhase.Announcement;

            // This will contain the new public keys.
            Map<VerificationKey, Message> encryptonKeys = new HashMap<>();

            // Everyone except player 1 creates a new keypair and sends it around to everyone else.
            DecryptionKey dk = null;
            EncryptionKey ek;
            if (me != 1) {
                dk = crypto.DecryptionKey();
                ek = dk.EncryptionKey();

                // Broadcast the key and store it in the set with everyone else's.
                encryptonKeys.put(pk, messages.make(τ, phase, sk).attach(ek));
                network.broadcast(messages.make(τ, phase, sk).attach(ek));
            }

            // Now we wait to receive similar inbox from everyone else.
            encryptonKeys.putAll(network.receiveFromMultiple(network.opponentSet(2, N), phase));

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
            Message σ2 = messages.make(τ, phase, sk);
            if (me != 1) {
                σ2.attach(network.receiveFrom(players[me - 2], phase));
                σ2 = decryptAll(dk, σ2, sk);
            }

            // Add our own address to the mix. Note that if me == N, ie, the last player, then no
            // encryption is done. That is because we have reached the last layer of encryption.
            Message encrypted = messages.make(τ, phase, sk).attach(vk_new);
            for (int i = me; i < N; i++) {
                // Successively encrypt with the keys of the players who haven't had their turn yet.
                encryptonKeys.get(players[i]).removeEncryptionKey().encrypt(encrypted);
            }

            // Insert new entry and reorder the keys.
            σ2 = shuffle(σ2.attach(encrypted), sk);

            // Pass it along to the next player.
            if (me != N) {
                network.sendTo(players[me], σ2);
            }

        // Phase 3: broadcast outputs.
            // In this phase, the last player just broadcasts the transaction to everyone else.
            phase = ShufflePhase.BroadcastOutput;

            Queue<VerificationKey> newAddresses;
            if (me == N) {
                // The last player adds his own new address in without encrypting anything and shuffles the result.
                Message σ3 = messages.make(τ, phase, sk).attach(σ2);
                newAddresses = readNewAddresses(σ2);
                network.broadcast(σ3);
            } else {
                newAddresses = readNewAddresses(network.receiveFrom(players[N - 1], phase));
            }

            // Everyone else receives the broadcast and checks to make sure their message was included.
            if (!newAddresses.contains(vk_new)) {
                throw new BlameException();
            }

        // Phase 4: equivocation check.
        // In this phase, participants check whether any player has sent different
        // encryption keys to different players.
        phase = ShufflePhase.EquivocationCheck;

            // Put all temporary encryption keys into a list and hash the result.
            Message σ4 = messages.make(τ, phase, sk);
            for (int i = 1; i < N; i++) {
                EncryptionKey kkk = encryptonKeys.get(players[i]).removeEncryptionKey();
                σ4.attach(kkk);
            }

            crypto.hash(σ4);
            network.broadcast(σ4);

            // Wait for a similar message from everyone else and check that the result is the name.
            Map<VerificationKey, Message> hashes = network.receiveFromMultiple(network.opponentSet(1, N), phase);
            hashes.put(pk, σ4);
            if (!areEqual(hashes)) {
                throw new BlameException();
            }

        // Phase 5: verification and submission.
        // Everyone creates a Bitcoin transaction and signs it, then broadcasts the signature.
        // If all signatures check out, then the transaction is sent into the network.
        phase = ShufflePhase.VerificationAndSubmission;

            List<VerificationKey> inputs = new LinkedList<>();
            LinkedHashMap<VerificationKey, CoinAmount> outputs = new LinkedHashMap<>();
            Collections.addAll(inputs, players);
            for(VerificationKey key : newAddresses) {
                outputs.put(key, ν);
            }
            CoinTransaction t = coin.transaction(inputs, outputs);
            network.broadcast(messages.make(τ, phase, sk).attach(sk.makeSignature(t)));

            Map<VerificationKey, Message> σ5 = network.receiveFromMultiple(network.opponentSet(1, N), phase);

            // Verify the signatures.
            for(Map.Entry<VerificationKey, Message> sig : σ5.entrySet()) {
                if (!sig.getKey().verify(t, sig.getValue().removeCoinSignature())) {
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
            e.printStackTrace();
            // Protocol does not actually work until this section is filled in.
        } catch (BlameReceivedException e) {
            phase = ShufflePhase.Blame;
            // TODO this is where we go if we hear tell of malicious behavior from a third player.
            e.printStackTrace();
        }
    }

    // the phase can be accessed concurrently in case we want to update
    // the user on how the protocol is going.
    public ShufflePhase currentPhase() {
        return phase;
    }

    // The function for public consumption which runs the protocol.
    // TODO Coming soon!! handle all these error states more delicately.
    public ReturnState run(CoinAmount ν, SigningKey sk, VerificationKey players[]) throws InvalidImplementationException, InterruptedException {

        // Don't let the protocol be run more than once at a time.
        if (phase != ShufflePhase.Uninitiated) {
            return new ReturnState(false, this.τ, currentPhase(), new ProtocolStartedException(), null);
        }

        if (τ == null || ν == null || sk == null || players == null) {
            return new ReturnState(false, null, currentPhase(), new NullPointerException(), null);
        }

        // Set up interactions with the shuffle network.
        network = new NetworkOperations(τ, sk, players, connection);

        // Here we handle a bunch of lower level errors.
        try {
            protocolDefinition(ν, sk, players);

            ShufflePhase endPhase = currentPhase();
            if (endPhase == ShufflePhase.Blame) {
                return new ReturnState(false, τ, ShufflePhase.Blame, null, blame);
            }
            return new ReturnState(true, τ, endPhase, null, null);
        } catch ( InvalidParticipantSetException
                | ValueException
                | MempoolException
                | BlockchainException
                | CoinNetworkException
                | CryptographyException
                | TimeoutException
                | FormatException e) {

            return new ReturnState(false, τ, currentPhase(), e, null);
        }

    }

    Queue<VerificationKey> readNewAddresses(Message message) throws FormatException, InvalidImplementationException {
        Queue<VerificationKey> queue = new LinkedList<>();

        Message element;
        while((element = message.poll()) != null) {
            queue.add(element.removeVerificationKey());
        }

        return queue;
    }

    Message decryptAll(DecryptionKey key, Message message, SigningKey sk) throws InvalidImplementationException, FormatException {
        Message decrypted = messages.make(τ, phase, sk);

        Message element;
        while((element = message.poll()) != null) {
            key.decrypt(element);
            decrypted.attach(element);
        }

        return decrypted;
    }

    // Algorithm to randomly shuffle a linked list.
    Message shuffle(Message σ, SigningKey sk) throws CryptographyException, InvalidImplementationException, FormatException {
        // Read all elements of the packet and insert them in a Queue.
        Queue<Message> old = new LinkedList<>();
        Message shuffled = messages.make(τ, phase, sk);
        Message element;
        while((element = σ.poll()) != null) {
            old.add(element);
        }

        int N = old.size();


        // Then successively and randomly select which one will be inserted until none remain.
        for (int i = N; i > 0; i --) {
            // Get a random number between 0 and N - 1 inclusive.
            int n = crypto.getRandom(i - 1);

            for (int j = 0; j < n; j ++) {
                old.add(old.remove());
            }

            // add the randomly selected element to the queue.
            shuffled.attach(old.remove());
        }

        return shuffled;
    }

    static boolean areEqual(Map<VerificationKey, Message> messages) throws InvalidImplementationException {
        boolean equal = true;

        Message last = null;
        for (Map.Entry<VerificationKey, Message> e : messages.entrySet()) {
            if (last != null) {
                equal = (equal&&last.equals(e.getValue()));
                if (!equal) {
                    return false;
                }
            }

            last = e.getValue();
        }

        return equal;
    }
}
