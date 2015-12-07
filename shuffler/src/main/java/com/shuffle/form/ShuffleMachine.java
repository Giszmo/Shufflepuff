package com.shuffle.form;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

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
    SessionIdentifier τ;

    Crypto crypto;

    Coin coin;

    PacketFactory packets;

    Network connection;

    NetworkOperations network;

    volatile ShufflePhase phase;

    public ShuffleMachine(
            PacketFactory packets,
            Crypto crypto,
            Network connection,
            Coin coin) {

        this.crypto = crypto;
        this.coin = coin;
        this.packets = packets;
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
            BlockChainException,
            MempoolException,
            InvalidImplementationException,
            ValueException,
            CoinNetworkException,
            InvalidParticipantSetException {

        int me = -1;
        int N = players.length;

        // Determine what my index number is.
        for (int i = 1; i <= N; i ++) {
            if (players[i - 1].equals(sk.VerificationKey())) {
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
            for(VerificationKey participant : players) {
                if (!coin.valueHeld(participant).greater(ν)) {
                    throw new BlameException();
                }
            }

        // Phase 1: Announcement
        // In the announcement phase, participants distribute temporary encryption keys.
        phase = ShufflePhase.Announcement;

            // This will contain the new public keys.
            Map<VerificationKey, Packet> encryptonKeys = new TreeMap<>();

            // Everyone except player 1 creates a new keypair and sends it around to everyone else.
            DecryptionKey dk = null;
            EncryptionKey ek;
            if (me != 1) {
                dk = crypto.DecryptionKey();
                ek = dk.EncryptionKey();

                // Broadcast the key and store it in the set with everyone else's.
                encryptonKeys.put(sk.VerificationKey(), packets.make().append(ek));
                network.broadcast(packets.make().append(ek));

            }

            // Now we wait to receive similar messages from everyone else.
            encryptonKeys.putAll(network.receiveFrom(network.opponentSet(2, N)));

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
            Packet σ2 = packets.make();
            if (me != 1) {
                σ2 = network.receiveFrom(players[me - 2]);
                decryptAll(dk, σ2);
            }

            // Add our own address to the mix. Note that if me == N, ie, the last player, then no
            // encryption is done. That is because we have reached the last layer of encryption.
            Packet encrypted = packets.make().append(vk_new);
            for (int i = me; i < N; i++) {
                // Successively encrypt with the keys of the players who haven't had their turn yet.
                encrypted = encryptonKeys.get(players[i]).readEncryptionKey().encrypt(encrypted);
            }

            // Insert new entry and reorder the keys.
            σ2.append(encrypted);
            σ2 = shuffle(σ2);

            // Pass it along to the next player.
            if (me != N) {
                network.sendTo(players[me], σ2);
            }

        // Phase 3: broadcast outputs.
        // In this phase, the last player just broadcasts the transaction to everyone else.
        phase = ShufflePhase.BroadcastOutput;

            Queue<VerificationKey> newAddresses;
            if (me == N) {
                newAddresses = readNewAddresses(σ2);
                network.broadcast(σ2);
            } else {
                newAddresses = readNewAddresses(network.receiveFrom(players[N - 1]));
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
            Packet σ4 = packets.make();
            for (int i = 1; i < N; i++) {
                σ4.append(encryptonKeys.get(players[i - 1]).readEncryptionKey());
            }

            σ4 = crypto.hash(σ4);
            network.broadcast(σ4);

            // Wait for a similar message from everyone else and check that the result is the name.
            Map<VerificationKey, Packet> hashes = network.receiveFrom(network.opponentSet(1, N));
            hashes.put(sk.VerificationKey(), σ4);
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
            network.broadcast(packets.make().append(sk.makeSignature(t)));

            Map<VerificationKey, Packet> σ5 = network.receiveFrom(network.opponentSet(1, N));

            // Verify the signatures.
            for(Map.Entry<VerificationKey, Packet> sig : σ5.entrySet()) {
                if (!sig.getKey().verify(t, sig.getValue().readCoinSignature())) {
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
            // Protocol does not actually work until this section is filled in.
        }
    }

    // the phase can be accessed concurrently in case we want to update
    // the user on how the protocol is going.
    public ShufflePhase currentPhase() {
        return phase;
    }

    // The function for public consumption which runs the protocol.
    // TODO Coming soon!! handle all these error states more delicately.
    public ShuffleErrorState run(SessionIdentifier τ, CoinAmount ν, SigningKey sk, VerificationKey players[]) {

        // Don't let the protocol be run more than once at a time.
        if (phase != ShufflePhase.Uninitiated) {
            return new ShuffleErrorState(this.τ, currentPhase(), new ProtocolStartedException());
        }

        // Set up interactions with the shuffle network.
        NetworkOperations network = new NetworkOperations(τ, sk, players, connection, this);

        // Here we handle a bunch of lower level errors.
        try {
            protocolDefinition(ν, sk, players);
        } catch ( InvalidImplementationException
                | InvalidParticipantSetException
                | ValueException
                | MempoolException
                | BlockChainException
                | CoinNetworkException
                | CryptographyException
                | TimeoutException
                | FormatException e) {

            phase = ShufflePhase.Uninitiated;
            return new ShuffleErrorState(τ, currentPhase(), e);
        }

        phase = ShufflePhase.Uninitiated;
        return null;
    }

    // TODO
    Queue<VerificationKey> readNewAddresses(Packet packet) {
        return null;
    }

    void decryptAll(DecryptionKey key, Packet packet) {
        // TODO
    }

    // Algorithm to randomly shuffle a linked list.
    Packet shuffle(Packet σ) throws CryptographyException, InvalidImplementationException, FormatException {
        // Read all elements of the packet and insert them in a Queue.
        Queue<Packet> old = new LinkedList<>();
        Packet shuffled = packets.make();
        Packet element;
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
            shuffled.append(old.remove());
        }

        return shuffled;
    }

    static boolean areEqual(Map<VerificationKey, Packet> messages) throws InvalidImplementationException {
        boolean equal = true;

        Packet last = null;
        for (Map.Entry<VerificationKey, Packet> e : messages.entrySet()) {
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
