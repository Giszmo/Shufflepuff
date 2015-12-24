package com.shuffle.protocol;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.SortedSet;
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
public final class ShuffleMachine {
    SessionIdentifier τ;

    int me;

    Crypto crypto;

    Coin coin;

    MessageFactory messages;

    Network connection;

    NetworkOperations network;

    ShufflePhase phase;

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

    BlameMatrix protocolDefinition(
            long amount, // The amount to be shuffled.
            SigningKey sk, // My signing key.
            Coin.Address myChange, // My change address. (may be null).
            Map<Integer, VerificationKey> players // The keys representing all the players, in order.
    ) throws
            TimeoutError,
            FormatException,
            CryptographyError,
            BlockchainError,
            MempoolError,
            InvalidImplementationError,
            ValueException,
            CoinNetworkError,
            InvalidParticipantSetException,
            InterruptedException {

        me = -1;
        int N = players.size();
        VerificationKey vk = sk.VerificationKey();

        // Determine what my index number is.
        for (int i = 1; i <= N; i++ ) {
            if (players.get(i).equals(vk)) {
                me = i;
            }
        }

        if (me < 0) {
            throw new InvalidParticipantSetException();
        }

        // The protocol is surrounded by a try block that catches BlameExceptions, which are thrown
        // when any other player deliberately breaks the rules.
        try {

        // Phase 1: Announcement
        // In the announcement phase, participants distribute temporary encryption keys.
        phase = ShufflePhase.Announcement;

            // Check for sufficient funds.
            // There was a problem with the wording of the original paper which would have meant
            // that player 1's funds never would have been checked, but we have to do that.
            BlameMatrix matrix = blameInsufficientFunds(players, amount, vk);
            if (matrix != null) {
                return matrix;
            }

            // This will contain the new public keys.
            Map<VerificationKey, EncryptionKey> encryptonKeys = new HashMap<>();

            // This will contain the change addresses.
            Map<VerificationKey, Coin.Address> change = new HashMap<>();

            // Everyone except player 1 creates a new keypair and sends it around to everyone else.
            DecryptionKey dk = null;
            EncryptionKey ek;
            if (me != 1) {
                dk = crypto.DecryptionKey();
                ek = dk.EncryptionKey();

                // Broadcast the key and store it in the set with everyone else's.
                encryptonKeys.put(vk, ek);
                change.put(vk, myChange);
                network.broadcast(messages.make().attach(ek).attach(myChange), phase, vk);
            }

            // Now we wait to receive similar inbox from everyone else.
            Map<VerificationKey, Message> σ1 = network.receiveFromMultiple(network.opponentSet(2, N), phase);
            readAnouncements(σ1, encryptonKeys, change);

        // Phase 2: Shuffle
        // In the shuffle phase, we create a sequence of orderings which will b successively
        // applied by each particpant. Everyone has the incentive to insert their own address
        // at a random location, which is sufficient to ensure randomness of the whole thing
        // to all participants.
        phase = ShufflePhase.Shuffling;

            // Each participant chooses a new bitcoin address which will be their new outputs.
            SigningKey sk_new = crypto.SigningKey();
            Coin.Address addr_new = sk_new.VerificationKey().address();

            // Player one begins the cycle and encrypts its new address with everyone's key, in order.
            // Each subsequent player reorders the cycle and removes one layer of encryption.
            Message σ2 = messages.make();
            if (me != 1) {
                σ2.attach(network.receiveFrom(players.get(me - 1), phase));
                σ2 = decryptAll(σ2, dk);
            }

            // Add our own address to the mix. Note that if me == N, ie, the last player, then no
            // encryption is done. That is because we have reached the last layer of encryption.
            Coin.Address encrypted = addr_new;
            for (int i = N; i > me; i--) {
                // Successively encrypt with the keys of the players who haven't had their turn yet.
                encrypted = encryptonKeys.get(players.get(i)).encrypt(encrypted);
            }

            // Insert new entry and reorder the keys.
            σ2 = shuffle(σ2.attach(encrypted));

            // Pass it along to the next player.
            if (me != N) {
                network.send(new Packet(σ2, τ, phase, vk, players.get(me + 1)));
            }

        // Phase 3: broadcast outputs.
        // In this phase, the last player just broadcasts the transaction to everyone else.
        phase = ShufflePhase.BroadcastOutput;

            Queue<Coin.Address> newAddresses;
            if (me == N) {
                // The last player adds his own new address in without encrypting anything and shuffles the result.
                newAddresses = readNewAddresses(σ2);
                network.broadcast(σ2, phase, vk);
            } else {
                newAddresses = readNewAddresses(network.receiveFrom(players.get(N), phase));
            }

            // Everyone else receives the broadcast and checks to make sure their message was included.
            if (!newAddresses.contains(addr_new)) {
                throw new BlameException(phase);
            }

        // Phase 4: equivocation check.
        // In this phase, participants check whether any player has sent different
        // encryption keys to different players.
        phase = ShufflePhase.EquivocationCheck;

            // Put all temporary encryption keys into a list and hash the result.
            Message σ4 = messages.make();
            for (int i = 1; i < N; i++) {
                σ4.attach(encryptonKeys.get(players.get(i+1)));
            }

            σ4 = crypto.hash(σ4);
            network.broadcast(σ4, phase, vk);

            // Wait for a similar message from everyone else and check that the result is the name.
            Map<VerificationKey, Message> hashes = network.receiveFromMultiple(network.opponentSet(1, N), phase);
            hashes.put(vk, σ4);
            if (!areEqual(hashes)) {
                throw new BlameException(phase);
            }

        // Phase 5: verification and submission.
        // Everyone creates a Bitcoin transaction and signs it, then broadcasts the signature.
        // If all signatures check out, then the transaction is sent into the network.
        phase = ShufflePhase.VerificationAndSubmission;

            List<Coin.Address> inputs = new LinkedList<>();
            for(int i = 1; i < N; i++) {
                inputs.add(players.get(i).address());
            }
            Coin.Transaction t = coin.shuffleTransaction(amount, inputs, newAddresses, change);
            network.broadcast(messages.make().attach(sk.makeSignature(t)), phase, vk);

            Map<VerificationKey, Message> σ5 = network.receiveFromMultiple(network.opponentSet(1, N), phase);

            // Verify the signatures.
            for(Map.Entry<VerificationKey, Message> sig : σ5.entrySet()) {
                if (!sig.getKey().verify(t, sig.getValue().readCoinSignature())) {
                    throw new BlameException(phase);
                }
            }

            // Check that the transaction is still valid.
            matrix = blameInsufficientFunds(players, amount, vk);
            if (matrix != null) {
                return matrix;
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

        return null;
    }

    // the phase can be accessed concurrently in case we want to update
    // the user on how the protocol is going.
    public ShufflePhase currentPhase() {
        return phase;
    }

    // The function for public consumption which runs the protocol.
    // TODO Coming soon!! handle all these error states more delicately.
    public ReturnState run(long amount, SigningKey sk, Coin.Address change, SortedSet<VerificationKey> players) throws InvalidImplementationError, InterruptedException {

        // Don't let the protocol be run more than once at a time.
        if (phase != ShufflePhase.Uninitiated) {
            return new ReturnState(false, this.τ, currentPhase(), new ProtocolStartedException(), null);
        }

        if (τ == null || sk == null || players == null) {
            return new ReturnState(false, null, currentPhase(), new NullPointerException(), null);
        }

        // Get the initial ordering of the players.
        int i = 1;
        Map<Integer, VerificationKey> numberedPlayers = new TreeMap<>();
        for (VerificationKey player : players) {
            numberedPlayers.put(i, player);
            i ++;
        }

        // Set up interactions with the shuffle network.
        network = new NetworkOperations(τ, sk, numberedPlayers, connection);

        // Here we handle a bunch of lower level errors.
        try {
            BlameMatrix blame = protocolDefinition(amount, sk, change, numberedPlayers);

            ShufflePhase endPhase = currentPhase();
            if (endPhase == ShufflePhase.Blame) {
                return new ReturnState(false, τ, ShufflePhase.Blame, null, blame);
            }
            return new ReturnState(true, τ, endPhase, null, null);
        } catch ( InvalidParticipantSetException
                | ValueException
                | MempoolError
                | BlockchainError
                | CoinNetworkError
                | CryptographyError
                | FormatException e) {

            return new ReturnState(false, τ, currentPhase(), e, null);
        } catch (TimeoutError e) {
            // TODO We have to go into "suspect" mode at this point to determine why the timeout occurred.
            return new ReturnState(false, τ, currentPhase(), e, null);
        }
    }

    void readAnouncements(Map<VerificationKey, Message> messages,
                          Map<VerificationKey, EncryptionKey> keys,
                          Map<VerificationKey, Coin.Address> change) throws FormatException {
        for (Map.Entry<VerificationKey, Message> entry : messages.entrySet()) {
            VerificationKey key = entry.getKey();
            Message message = entry.getValue();

            keys.put(key, message.readEncryptionKey());

            if (!message.isEmpty()) {
                change.put(key, message.readCoinAddress());
            }
        }
    }

    Queue<Coin.Address> readNewAddresses(Message message) throws FormatException, InvalidImplementationError {
        Queue<Coin.Address> queue = new LinkedList<>();

        Message copy = messages.copy(message);
        while(!copy.isEmpty()) {
            queue.add(copy.readCoinAddress());
        }

        return queue;
    }

    Message decryptAll(Message message, DecryptionKey key) throws InvalidImplementationError, CryptographyError, FormatException {
        Message decrypted = messages.make();

        Message copy = messages.copy(message);
        while(!copy.isEmpty()) {
            decrypted.attach(key.decrypt(copy.readCoinAddress()));
        }

        return decrypted;
    }

    // Algorithm to randomly shuffle a linked list.
    Message shuffle(Message σ) throws CryptographyError, InvalidImplementationError, FormatException {
        Message copy = messages.copy(σ);
        Message shuffled = messages.make();

        // Read all elements of the packet and insert them in a Queue.
        Queue<Coin.Address> old = new LinkedList<>();
        int N = 0;
        while(!copy.isEmpty()) {
            old.add(copy.readCoinAddress());
            N++;
        }

        // Then successively and randomly select which one will be inserted until none remain.
        for (int i = N; i > 0; i--) {
            // Get a random number between 0 and N - 1 inclusive.
            int n = crypto.getRandom(i - 1);

            for (int j = 0; j < n; j++) {
                old.add(old.remove());
            }

            // add the randomly selected element to the queue.
            shuffled.attach(old.remove());
        }

        return shuffled;
    }

    static boolean areEqual(Map<VerificationKey, Message> messages) throws InvalidImplementationError {
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

    BlameMatrix fillBlameMatrix(BlameMatrix matrix) throws InterruptedException, FormatException, ValueException {
        List<Packet> responses = network.receiveAllBlame();

        // Determine who is being blamed and by whom.
        for(Packet response : responses) {
            VerificationKey from = response.signer;
            Message message = response.message;

            while (!message.isEmpty()) {
                BlameMatrix.Blame blame = message.readBlame();
                // TODO determine what the blame is for and take the appropriate action.
                // Egad this is getting complicated.
            }
        }

        return matrix;
    }

    // Check for players with insufficient funds. This happens in phase 1 and phase 5.
    private BlameMatrix blameInsufficientFunds(Map<Integer, VerificationKey> players, long amount, VerificationKey vk) throws InterruptedException, FormatException, ValueException {
        List<VerificationKey> offenders = new LinkedList<>();

        // Check that each participant has the required amounts.
        for(VerificationKey player : players.values()) {
            if (coin.valueHeld(player.address()) < amount) {
                // Enter the blame phase.
                offenders.add(player);
            }
        }

        // If they do, return.
        if (offenders.isEmpty()) {
            return null;
        }

        // If not, enter blame phase and find offending transactions.
        phase = ShufflePhase.Blame;
        BlameMatrix matrix = new BlameMatrix();
        Message blameMessage = messages.make();
        for(VerificationKey offender : offenders) {
            Coin.Transaction t = coin.getOffendingTransaction(offender.address(), amount);

            if (t == null) {
                blameMessage.attach(new BlameMatrix.Blame(offender));
                matrix.add(vk, offender,
                        new BlameMatrix.BlameEvidence(BlameMatrix.BlameReason.NoFundsAtAll, true));
            } else {
                blameMessage.attach(new BlameMatrix.Blame(offender));
                matrix.add(vk, offender,
                        new BlameMatrix.BlameEvidence(BlameMatrix.BlameReason.InsufficientFunds, true, t));
            }
        }

        // Broadcast offending transactions.
        network.broadcast(blameMessage, phase, vk);

        // Get all subsequent blame messages.
        return fillBlameMatrix(matrix);
    }

    // The equivocation check fails and some player has equivocated.
    private BlameMatrix blameEquivocation() {
        return null;
    }

    // Some misbehavior that has occurred during the shuffle phase.
    private BlameMatrix blameShuffleMisbehavior() {
        return null;
    }
}
