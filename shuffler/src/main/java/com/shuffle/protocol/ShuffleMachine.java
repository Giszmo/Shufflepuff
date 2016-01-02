package com.shuffle.protocol;

import com.shuffle.cryptocoin.Address;
import com.shuffle.cryptocoin.BlockchainError;
import com.shuffle.cryptocoin.Coin;
import com.shuffle.cryptocoin.CoinNetworkError;
import com.shuffle.cryptocoin.Crypto;
import com.shuffle.cryptocoin.CryptographyError;
import com.shuffle.cryptocoin.DecryptionKey;
import com.shuffle.cryptocoin.EncryptionKey;
import com.shuffle.cryptocoin.MempoolError;
import com.shuffle.cryptocoin.SigningKey;
import com.shuffle.cryptocoin.Transaction;
import com.shuffle.cryptocoin.VerificationKey;

import java.net.ProtocolException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
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
            SigningKey sk, // My signing privateKey.
            Address myChange, // My change address. (may be null).
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
            InterruptedException, ProtocolException {

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
        Map<VerificationKey, Address> change = new HashMap<>();

        // Everyone except player 1 creates a new keypair and sends it around to everyone else.
        DecryptionKey dk = null;
        EncryptionKey ek;
        if (me != 1) {
            dk = crypto.DecryptionKey();
            ek = dk.EncryptionKey();

            // Broadcast the privateKey and store it in the set with everyone else's.
            encryptonKeys.put(vk, ek);
            change.put(vk, myChange);
            Message message = messages.make().attach(ek);
            if (myChange != null) {
                message.attach(myChange);
            }
            network.broadcast(message, phase, vk);
        }

        // Now we wait to receive similar inbox from everyone else.
        try {
            Map<VerificationKey, Message> σ1 = network.receiveFromMultiple(network.playerSet(2, N), phase);
            readAnouncements(σ1, encryptonKeys, change);
        } catch (BlameException e) {
            // TODO : might receive blame messages about insufficient funds.
        }

        // Phase 2: Shuffle
        // In the shuffle phase, we create a sequence of orderings which will b successively
        // applied by each particpant. Everyone has the incentive to insert their own address
        // at a random location, which is sufficient to ensure randomness of the whole thing
        // to all participants.
        phase = ShufflePhase.Shuffling;

        // The set of new addresses into which the coins will be deposited.
        Queue<Address> newAddresses = null;

        try {

            // Each participant chooses a new bitcoin address which will be their new outputs.
            SigningKey sk_new = crypto.SigningKey();
            Address addr_new = sk_new.VerificationKey().address();

            // Player one begins the cycle and encrypts its new address with everyone's privateKey, in order.
            // Each subsequent player reorders the cycle and removes one layer of encryption.
            Message σ2 = messages.make();
            if (me != 1) {
                σ2 = decryptAll(σ2.attach(network.receiveFrom(players.get(me - 1), phase)), dk, me - 1);
            }

            // Add our own address to the mix. Note that if me == N, ie, the last player, then no
            // encryption is done. That is because we have reached the last layer of encryption.
            Address encrypted = addr_new;
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

            if (me == N) {
                // The last player adds his own new address in without encrypting anything and shuffles the result.
                newAddresses = readNewAddresses(σ2);
                network.broadcast(σ2, phase, vk);
            } else {
                newAddresses = readNewAddresses(network.receiveFrom(players.get(N), phase));
            }

            // Everyone else receives the broadcast and checks to make sure their message was included.
            if (!newAddresses.contains(addr_new)) {
                // TODO handle this case.
            }

            // Phase 4: equivocation check.
            // In this phase, participants check whether any player has sent different
            // encryption keys to different players.
            phase = ShufflePhase.EquivocationCheck;

            matrix = equivocationCheck(players, encryptonKeys, vk);
            if (matrix != null) {
                return matrix;
            }
        } catch (BlameException e) {
            // TODO might receive messages about failed shuffles or failed equivocation check.
        }

        // Phase 5: verification and submission.
        // Everyone creates a Bitcoin transaction and signs it, then broadcasts the signature.
        // If all signatures check out, then the transaction is sent into the network.
        phase = ShufflePhase.VerificationAndSubmission;

        List<Address> inputs = new LinkedList<>();
        for(int i = 1; i < N; i++) {
            inputs.add(players.get(i).address());
        }
        Transaction t = coin.shuffleTransaction(amount, inputs, newAddresses, change);
        network.broadcast(messages.make().attach(sk.makeSignature(t)), phase, vk);

        try {
            Map<VerificationKey, Message> σ5 = network.receiveFromMultiple(network.playerSet(1, N), phase);

            // Verify the signatures.
            for(Map.Entry<VerificationKey, Message> sig : σ5.entrySet()) {
                if (!sig.getKey().verify(t, sig.getValue().readSignature())) {
                    // TODO handle this case.
                }
            }
        } catch (BlameException e) {
            // TODO might get something about invalid signatures.
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

        return null;
    }

    // the phase can be accessed concurrently in case we want to update
    // the user on how the protocol is going.
    public ShufflePhase currentPhase() {
        return phase;
    }

    // The function for public consumption which runs the protocol.
    public ReturnState run(long amount, SigningKey sk, Address change, SortedSet<VerificationKey> players,
                           int maxRetries, int minPlayers) throws InvalidImplementationError, InterruptedException {

        // Don't let the protocol be run more than once at a time.
        if (phase != ShufflePhase.Uninitiated) {
            return new ReturnState(false, this.τ, currentPhase(), new ProtocolStartedException(), null);
        }

        if (τ == null || sk == null || players == null) {
            return new ReturnState(false, null, currentPhase(), new NullPointerException(), null);
        }

        int attempt = 0;

        // The eliminated players.
        SortedSet<VerificationKey> eliminated = new TreeSet<>();

        // Here we handle a bunch of lower level errors.
        try {
            BlameMatrix blame = null;
            while (true) {

                if (players.size() - eliminated.size() < minPlayers) {
                    break;
                }

                // Get the initial ordering of the players.
                int i = 1;
                Map<Integer, VerificationKey> numberedPlayers = new TreeMap<>();
                for (VerificationKey player : players) {
                    if (!eliminated.contains(player)) {
                        numberedPlayers.put(i, player);
                        i ++;
                    }
                }

                // Set up interactions with the shuffle network.
                network = new NetworkOperations(τ, sk, numberedPlayers, connection);

                // Run the protocol.
                try {
                    blame = protocolDefinition(amount, sk, change, numberedPlayers);
                } catch (TimeoutError e) {
                    // TODO We have to go into "suspect" mode at this point to determine why the timeout occurred.
                    return new ReturnState(false, τ, currentPhase(), e, null);
                }

                ShufflePhase endPhase = currentPhase();

                if (endPhase != ShufflePhase.Blame) {
                    // The protocol was successful, so return.
                    return new ReturnState(true, τ, endPhase, null, null);
                }

                attempt++;

                if (attempt > maxRetries) {
                    break;
                }

                // Eliminate malicious players if possible and try again.
                phase = ShufflePhase.Uninitiated;

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

            return new ReturnState(false, τ, ShufflePhase.Blame, null, blame);
        } catch ( InvalidParticipantSetException
                | ProtocolException
                | ValueException
                | MempoolError
                | BlockchainError
                | CoinNetworkError
                | CryptographyError
                | FormatException e) {
            // TODO many of these cases could be dealt with instead of just aborting.
            return new ReturnState(false, τ, currentPhase(), e, null);
        }
    }

    void readAnouncements(Map<VerificationKey, Message> messages,
                          Map<VerificationKey, EncryptionKey> keys,
                          Map<VerificationKey, Address> change) throws FormatException {
        for (Map.Entry<VerificationKey, Message> entry : messages.entrySet()) {
            VerificationKey key = entry.getKey();
            Message message = entry.getValue();

            keys.put(key, message.readEncryptionKey());

            if (!message.isEmpty()) {
                change.put(key, message.readAddress());
            }
        }
    }

    Queue<Address> readNewAddresses(Message message) throws FormatException, InvalidImplementationError {
        Queue<Address> queue = new LinkedList<>();

        Message copy = messages.copy(message);
        while(!copy.isEmpty()) {
            queue.add(copy.readAddress());
        }

        return queue;
    }

    Message decryptAll(Message message, DecryptionKey key, int expected) throws InvalidImplementationError, FormatException {
        Message decrypted = messages.make();

        int count = 1;
        Set<Address> addrs = new HashSet<>(); // Used to check that all addresses are different.

        Message copy = messages.copy(message);
        while(!copy.isEmpty()) {
            Address address = copy.readAddress();
            addrs.add(address);
            count ++;
            try {
                decrypted.attach(key.decrypt(address));
            } catch (CryptographyError e) {
                // TODO Enter blame phase.
            }
        }

        if (addrs.size() != count) {
            // TODO enter blame phase.
        }

        return decrypted;
    }

    // There is an error case in which we have to do an equivocation check, so this phase is in a separate function.
    BlameMatrix equivocationCheck(
            Map<Integer, VerificationKey> players,
            Map<VerificationKey, EncryptionKey> encryptonKeys,
            VerificationKey vk) throws InterruptedException, ValueException, FormatException, ProtocolException, BlameException {
        // Put all temporary encryption keys into a list and hash the result.
        Message σ4 = messages.make();
        for (int i = 2; i <= players.size(); i++) {
            σ4.attach(encryptonKeys.get(players.get(i)));
        }

        σ4 = crypto.hash(σ4);
        network.broadcast(σ4, phase, vk);

        // Wait for a similar message from everyone else and check that the result is the name.
        Map<VerificationKey, Message> hashes = network.receiveFromMultiple(network.playerSet(1, players.size()), phase);
        hashes.put(vk, σ4);
        if (areEqual(hashes.values())) {
            return null;
        }

        // If the hashes are not equal, enter the blame phase.
        // Collect all packets from phase 1 and 3.
        phase = ShufflePhase.Blame;
        Message blameMessage = messages.make();
        List<Packet> evidence = network.getPacketsByPhase(ShufflePhase.Announcement);
        evidence.addAll(network.getPacketsByPhase(ShufflePhase.BroadcastOutput));
        blameMessage.attach(new BlameMatrix.Blame(evidence));
        network.broadcast(blameMessage, ShufflePhase.Blame, vk);

        return fillBlameMatrix(new BlameMatrix(), players, encryptonKeys, vk);
    }

    // Algorithm to randomly shuffle a linked list.
    Message shuffle(Message σ) throws CryptographyError, InvalidImplementationError, FormatException {
        Message copy = messages.copy(σ);
        Message shuffled = messages.make();

        // Read all elements of the packet and insert them in a Queue.
        Queue<Address> old = new LinkedList<>();
        int N = 0;
        while(!copy.isEmpty()) {
            old.add(copy.readAddress());
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

    static boolean areEqual(Iterable<Message> messages) throws InvalidImplementationError {
        boolean equal = true;

        Message last = null;
        for (Message m : messages) {
            if (last != null) {
                equal = (equal&&last.equals(m));
                if (!equal) {
                    return false;
                }
            }

            last = m;
        }

        return equal;
    }

    // When we know we'll receive a bunch of blame messages, we have to go through them all to figure
    // out what's going on.
    BlameMatrix fillBlameMatrix(BlameMatrix matrix,
                                Map<Integer, VerificationKey> players,
                                Map<VerificationKey, EncryptionKey> encryptonKeys,
                                VerificationKey vk) throws InterruptedException, FormatException, ValueException {
        Map<VerificationKey, List<Packet>> blameMessages = network.receiveAllBlame();

        // Get all hashes received in phase 4 so that we can check that they were reported correctly.
        List<Packet> hashMessages = network.getPacketsByPhase(ShufflePhase.EquivocationCheck);
        Map<VerificationKey, Message> hashes = new HashMap<>();
        for(Packet packet : hashMessages) {
            hashes.put(packet.signer, packet.message);
        }

        // The messages sent in the broadcast phase by the last player to all the other players.
        Map<VerificationKey, Message> outputVectors = new HashMap<>();

        // The encryption keys sent from every player to every other.
        Map<VerificationKey, Map<VerificationKey, EncryptionKey>> sentKeys = new HashMap<>();

        // The list of messages sent in phase 2.
        Map<VerificationKey, Message> shuffleMessages = new HashMap<>();

        // The set of decryption keys from each player.
        Map<VerificationKey, DecryptionKey> decryptionKeys = new HashMap<>();

        // Determine who is being blamed and by whom.
        for (Map.Entry<VerificationKey, List<Packet>> entry : blameMessages.entrySet()) {
            VerificationKey from = entry.getKey();
            List<Packet> responses = entry.getValue();
            for (Packet packet : responses) {
                Message message = packet.message;

                while (!message.isEmpty()) {
                    BlameMatrix.Blame blame = message.readBlame();
                    boolean credible;
                    switch (blame.reason) {
                        case NoFundsAtAll:
                            if (from.equals(vk)) {
                                break; // Skip, this is mine.
                            }
                            // Do we already know about this? The evidence is not credible if we don't.
                            credible = matrix.blameExists(vk, blame.accused, BlameMatrix.BlameReason.NoFundsAtAll);
                            matrix.add(blame.accused, from,
                                    new BlameMatrix.BlameEvidence(BlameMatrix.BlameReason.NoFundsAtAll, credible));
                            break;
                        case InsufficientFunds:
                            if (from.equals(vk)) {
                                break; // Skip, this is mine.
                            }
                            // Is the evidence included sufficient?
                            credible = coin.isOffendingTransaction(blame.accused.address(), blame.t);
                            matrix.add(blame.accused, from,
                                    new BlameMatrix.BlameEvidence(BlameMatrix.BlameReason.NoFundsAtAll, credible));
                            break;
                        case EquivocationFailure:
                            Map<VerificationKey, EncryptionKey> receivedKeys = new HashMap<>();

                            // Collect all packets received in the appropriate place.
                            for (Packet received : blame.packets) {
                                switch (received.phase) {
                                    case BroadcastOutput:
                                        if(outputVectors.containsKey(from)) {
                                            // TODO blame someone here.
                                        }
                                        outputVectors.put(from, received.message);
                                        break;
                                    case Announcement:
                                        Map<VerificationKey, EncryptionKey> map = sentKeys.get(received.signer);
                                        if (map == null) {
                                            map = sentKeys.put(received.signer, new HashMap<VerificationKey, EncryptionKey>());
                                        }

                                        EncryptionKey key = received.message.readEncryptionKey();
                                        map.put(from, key);
                                        receivedKeys.put(from, key);
                                        break;
                                    default:
                                        // TODO this case should never happen. If it does, could it be malicious?
                                        break;
                                }
                            }

                            // Check on whether this player correctly reported the hash.
                            Message σ4 = messages.make();
                            for (int i = 2; i <= players.size(); i++) {
                                σ4.attach(receivedKeys.get(players.get(i)));
                            }

                            if (!hashes.get(packet.signer).equals(crypto.hash(σ4))) {
                                matrix.add(vk, from, null /* TODO */);
                            }

                            break;
                        case ShuffleFailure:
                            if(decryptionKeys.containsKey(from)) {
                                // TODO blame someone here.
                            }
                            decryptionKeys.put(from, blame.privateKey);

                            // Check that the decryption key is valid.
                            if (encryptonKeys != null) {
                                if(!blame.privateKey.EncryptionKey().equals(encryptonKeys.get(from))) {
                                    // TODO blame someone here.
                                }
                            }

                            // Collect all packets received in the appropriate place.
                            for (Packet received : blame.packets) {
                                switch (received.phase) {
                                    case BroadcastOutput:
                                        if(outputVectors.containsKey(packet.signer)) {
                                            // TODO blame someone here.
                                        }
                                        outputVectors.put(packet.signer, received.message);
                                        break;
                                    case Shuffling:
                                        if(shuffleMessages.containsKey(packet.signer)) {
                                            // TODO blame someone here.
                                        }
                                        shuffleMessages.put(packet.signer, received.message);
                                        break;
                                    default:
                                        // TODO this case should never happen. If it does, could it be malicious?
                                        break;
                                }
                            }

                            break;
                    }
                }
            }
        }

        if (outputVectors.size() > 0) {
            // We should have one output vector for every player except the last.
            Set<VerificationKey> leftover = network.playerSet(1, players.size() - 1);
            leftover.removeAll(outputVectors.keySet());
            if (leftover.size() > 0) {
                // TODO blame someone.
            }

            // If they are not all equal, blame the last player for equivocating.
            if (!areEqual(outputVectors.values())) {
                // TODO blame last player.
            }
        }

        // Check that we have all the required announcement messages.
        if (sentKeys.size() > 0) {
            for (int i = 2; i < players.size(); i++) {
                VerificationKey from = players.get(i);

                Map<VerificationKey, EncryptionKey> sent = sentKeys.get(from);

                if (sent == null) {
                    // TODO this shouldn't be possible.
                }

                EncryptionKey key = null;
                for (int j = 1; j < players.size(); j++) {
                    VerificationKey to = players.get(i);

                    EncryptionKey next = sent.get(to);

                    if (next == null) {
                        // TODO blame player to. He should have sent us this.
                    }

                    if (key == null) {
                        key = next;
                    } else {
                        if (!key.equals(next)) {
                            // TODO blame player from for equivocating.
                            break;
                        }
                    }
                }
            }
        }

        if (decryptionKeys.size() > 0) {
            // We should have one decryption key for every player except the first.
            Set<VerificationKey> leftover = network.playerSet(1, players.size() - 1);
            leftover.removeAll(outputVectors.keySet());
            if (leftover.size() > 0) {
                // TODO blame someone.
            } else {
                // TODO Replay phase two.
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
            Transaction t = coin.getOffendingTransaction(offender.address(), amount);

            if (t == null) {
                blameMessage.attach(new BlameMatrix.Blame(offender));
                matrix.add(vk, offender,
                        new BlameMatrix.BlameEvidence(BlameMatrix.BlameReason.NoFundsAtAll, true));
            } else {
                blameMessage.attach(new BlameMatrix.Blame(offender, t));
                matrix.add(vk, offender,
                        new BlameMatrix.BlameEvidence(BlameMatrix.BlameReason.InsufficientFunds, true, t));
            }
        }

        // Broadcast offending transactions.
        network.broadcast(blameMessage, phase, vk);

        // Get all subsequent blame messages.
        return fillBlameMatrix(matrix, players, null, vk);
    }

    // Some misbehavior that has occurred during the shuffle phase.
    private BlameMatrix blameShuffleMisbehavior(
            Map<Integer, VerificationKey> players,
            Map<VerificationKey, EncryptionKey> encryptonKeys,
            DecryptionKey dk,
            VerificationKey vk) throws InterruptedException, FormatException, ValueException, ProtocolException, BlameException {
        // First skip to phase 4 and do an equivocation check.
        phase = ShufflePhase.EquivocationCheck;
        BlameMatrix matrix = equivocationCheck(players, encryptonKeys, vk);

        // If we get a blame matrix back, that means that the culprit was found.
        if (matrix != null) {
            return matrix;
        }

        // Otherwise, there are some more things we have to check.
        phase = ShufflePhase.Blame;

        // Collect all packets from phase 2 and 3.
        Message blameMessage = messages.make();
        List<Packet> evidence = network.getPacketsByPhase(ShufflePhase.Shuffling);
        evidence.addAll(network.getPacketsByPhase(ShufflePhase.BroadcastOutput));

        // Send them all with the decryption key.
        blameMessage.attach(new BlameMatrix.Blame(dk, evidence));
        network.broadcast(blameMessage, ShufflePhase.Blame, vk);

        return fillBlameMatrix(new BlameMatrix(), players, encryptonKeys, vk);
    }
}
