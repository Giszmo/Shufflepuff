/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Coin;
import com.shuffle.bitcoin.CoinNetworkException;
import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.Send;
import com.shuffle.p2p.Bytestring;
import com.shuffle.protocol.blame.Blame;
import com.shuffle.protocol.blame.BlameException;
import com.shuffle.protocol.blame.Evidence;
import com.shuffle.protocol.blame.Matrix;
import com.shuffle.protocol.blame.Reason;
import com.shuffle.protocol.message.Message;
import com.shuffle.protocol.message.MessageFactory;
import com.shuffle.protocol.message.Packet;
import com.shuffle.protocol.message.Phase;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.util.Deque;
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
 * The ShuffleMachine class is the abstract state machine that defines the CoinShuffle protocol. All
 * protocols ultimately are definitions of abstract state machines, so that is what this class is.
 *
 * Created by Daniel Krawisz on 12/3/15.
 *
 */
public class CoinShuffle {
    private static final Logger log = LogManager.getLogger(CoinShuffle.class);

    final Crypto crypto;

    private final Coin coin;

    final MessageFactory messages;

    // A single round of the protocol. It is possible that the players may go through
    // several failed rounds until they have eliminated malicious players.
    class Round {
        final CurrentPhase phase;

        private final long amount; // The amount to be shuffled.

        final SigningKey sk; // My signing private key.

        public final int me; // Which player am I?

        public final Map<Integer, VerificationKey> players; // The players' public keys.

        public final int N; // The number of players.

        public final VerificationKey vk; // My verification public key, which is also my identity.

        public DecryptionKey dk = null;

        // This will contain the new encryption public keys.
        public final Map<VerificationKey, EncryptionKey> encryptionKeys = new HashMap<>();

        // The set of new addresses into which the coins will be deposited.
        public Queue<Address> newAddresses = null;

        public final Address addrNew;

        public final Address change; // My change address. (may be null).

        public final Map<VerificationKey, Bytestring> signatures = new HashMap<>();

        public final Mailbox mailbox;

        Transaction protocolDefinition(
        ) throws WaitingException, Matrix, InterruptedException,
                FormatException, IOException, CoinNetworkException {

            if (amount <= 0) {
                throw new IllegalArgumentException();
            }

            // Phase 1: Announcement
            // In the announcement phase, participants distribute temporary encryption keys.
            phase.set(Phase.Announcement);
            log.info(" * Player " + me + " begins Coin Shuffle protocol with " + N + " players.");

            // Check for sufficient funds.
            // There was a problem with the wording of the original paper which would have meant
            // that player 1's funds never would have been checked, but we have to do that.
            blameInsufficientFunds();

            // This will contain the change addresses.
            Map<VerificationKey, Address> changeAddresses = new HashMap<>();

            // Everyone except player 1 creates a new keypair and sends it around to everyone else.
            dk = broadcastNewKey(changeAddresses);

            // Now we wait to receive similar key from everyone else.
            Map<VerificationKey, Message> announcement;
            try {
                announcement = mailbox.receiveFromMultiple(playerSet(2, N), phase.get());
            } catch (BlameException e) {
                // might receive blame messages about insufficient funds.
                phase.set(Phase.Blame);
                throw fillBlameMatrix();
            }

            readAnnouncements(announcement, encryptionKeys, changeAddresses);

            // Phase 2: Shuffle
            // In the shuffle phase, players go in order and reorder the addresses they have been
            // given by the previous player. They insert their own address in a random location.
            // Everyone has the incentive to insert their own address at a random location, which
            // is sufficient to ensure that the result appears random to everybody.
            phase.set(Phase.Shuffling);

            try {
                // Player one begins the cycle and encrypts its new address with everyone's
                // public encryption key, in order.
                // Each subsequent player reorders the cycle and removes one layer of encryption.
                Message shuffled;
                if (me != 1) {
                    shuffled = decryptAll(
                            mailbox.receiveFrom(players.get(me - 1), phase.get()), dk, me - 1);

                    if (shuffled == null) {
                        blameShuffleMisbehavior();
                    }
                } else {
                    shuffled = messages.make();
                }

                // Make an encrypted address to the mix, and then shuffle everything ourselves.
                shuffled = shufflePhase(shuffled, addrNew);

                // Pass it along to the next player.
                if (me != N) {
                    mailbox.send(shuffled, phase.get(), players.get(me + 1));
                }

                // Phase 3: broadcast outputs.
                // In this phase, the last player just broadcasts the transaction to everyone else.
                phase.set(Phase.BroadcastOutput);

                newAddresses = readAndBroadcastNewAddresses(shuffled);
            } catch (BlameException e) {
                switch (e.packet.payload().readBlame().reason) {
                    case MissingOutput: {
                        // This was sent by a player in phase 3, which means that the new addresses
                        // were sent out by the last player, which means that we need to receive
                        // the new addresses before proceeding.
                        if (newAddresses == null) {
                            newAddresses = readNewAddresses(mailbox.receiveFromBlameless(
                                    players.get(N), phase.get()));
                        }
                        // Continue on to next case.
                    }
                    case ShuffleFailure: {
                        // This was sent
                        blameShuffleMisbehavior();
                    }
                    default: {
                        throw fillBlameMatrix();
                    }
                }
            }

            // Everyone else receives the broadcast and checks that their message was included.
            if (!newAddresses.contains(addrNew)) {
                phase.set(Phase.Blame);
                mailbox.broadcast(messages.make().attach(Blame.MissingOutput(players.get(N))),
                        phase.get());

                blameShuffleMisbehavior();
            }

            // Phase 4: equivocation check.
            // In this phase, participants check whether any player has history different
            // encryption keys to different players.
            phase.set(Phase.EquivocationCheck);

            equivocationCheck(encryptionKeys, newAddresses, false);

            // Phase 5: verification and submission.
            // Everyone creates a Bitcoin transaction and signs it, then broadcasts the signature.
            // If all signatures check out, then the transaction is history into the net.
            phase.set(Phase.VerificationAndSubmission);

            List<VerificationKey> inputs = new LinkedList<>();
            for (int i = 1; i <= N; i++) {
                inputs.add(players.get(i));
            }

            Transaction t = coin.shuffleTransaction(amount, inputs, newAddresses, changeAddresses);
            
            checkDoubleSpending(t);

            mailbox.broadcast(messages.make().attach(sk.sign(t.serialize())), phase.get());

            Map<VerificationKey, Message> signatureMessages = null;
            boolean invalidClaim = false;
            try {
                signatureMessages = mailbox.receiveFromMultiple(playerSet(1, N), phase.get());
            } catch (BlameException e) {
                switch (e.packet.payload().readBlame().reason) {
                    case InvalidSignature: {
                        // Continue receiving messages and ignore any further blame messages.
                        signatureMessages = mailbox.receiveFromMultipleBlameless(playerSet(1, N),
                                phase.get());

                        invalidClaim = true;
                        break;
                    }
                    case DoubleSpend:
                    default: {
                        phase.set(Phase.Blame);
                        // Could receive notice of double spending here.
                        throw fillBlameMatrix();
                    }
                }
            }

            // Verify the signatures.
            Map<VerificationKey, Bytestring> invalid = new HashMap<>();
            for (Map.Entry<VerificationKey, Message> sig : signatureMessages.entrySet()) {
                VerificationKey key = sig.getKey();
                Bytestring signature = sig.getValue().readSignature();
                signatures.put(key, signature);
                if (!key.verify(t.serialize(), signature)) {
                    invalid.put(key, signature);
                }
            }

            if (invalid.size() > 0 || invalidClaim) {
                phase.set(Phase.Blame);
                Message blameMessage = messages.make();

                for (Map.Entry<VerificationKey, Bytestring> bad : invalid.entrySet()) {
                    VerificationKey key = bad.getKey();
                    Bytestring signature = bad.getValue();

                    blameMessage = blameMessage.attach(Blame.InvalidSignature(key, signature));
                }
                mailbox.broadcast(blameMessage, phase.get());
                throw fillBlameMatrix();
            }

            // Send the transaction into the net.
            t.send();

            // The protocol has completed successfully.
            phase.set(Phase.Completed);

            return t;
        }

        // Everyone except player 1 creates a new keypair and sends it around to everyone else.
        DecryptionKey broadcastNewKey(Map<VerificationKey, Address> changeAddresses)
                throws WaitingException, InterruptedException, IOException {

            DecryptionKey dk = null;
            if (me != 1) {
                dk = crypto.makeDecryptionKey();

                // Broadcast the public key and store it in the set with everyone else's.
                encryptionKeys.put(vk, dk.EncryptionKey());
                changeAddresses.put(vk, change);
                Message message = messages.make().attach(dk.EncryptionKey());
                if (change != null) {
                    message.attach(change);
                }

                mailbox.broadcast(message, phase.get());
            }
            return dk;
        }

        Deque<Address> readNewAddresses(Message message)
                throws FormatException, InvalidImplementationError {

            Deque<Address> queue = new LinkedList<>();

            while (!message.isEmpty()) {
                queue.add(message.readAddress());
                message = message.rest();
            }

            return queue;
        }

        // The shuffle phase.
        Message shufflePhase(Message shuffled, Address addrNew) throws FormatException {

            // Add our own address to the mix. Note that if me == N, ie, the last player, then no
            // encryption is done. That is because we have reached the last layer of encryption.
            Address encrypted = addrNew;
            for (int i = N; i > me; i--) {
                // Successively encrypt with the keys of the players who haven't had their turn yet.
                encrypted = encryptionKeys.get(players.get(i)).encrypt(encrypted);
            }

            // Insert new entry and reorder the keys.
            return shuffle(shuffled.attach(encrypted));
        }

        // In the broadcast phase, we have to either receive all the
        // new addresses or send them all out. This is set off in its own function so that the
        // malicious machine can override it.
        Deque<Address> readAndBroadcastNewAddresses(Message shuffled)
                throws IOException, InterruptedException,
                WaitingException, BlameException, FormatException {

            Deque<Address> newAddresses;
            if (me == N) {
                // The last player adds his own new address in without
                // encrypting anything and shuffles the result.
                newAddresses = readNewAddresses(shuffled);
                mailbox.broadcast(shuffled, phase.get());
            } else {
                newAddresses = readNewAddresses(mailbox.receiveFrom(players.get(N), phase.get()));
            }

            return newAddresses;
        }

        // In the shuffle phase, we have to receive a set of strings from the previous player and
        // decrypt them all.
        final Message decryptAll(Message message, DecryptionKey key, int expected)
                throws IOException, InterruptedException, FormatException {

            Message decrypted = messages.make();

            int count = 0;
            Set<Address> addrs = new HashSet<>(); // Used to check that all addresses are different.

            while (!message.isEmpty()) {
                Address address = message.readAddress();
                message = message.rest();

                addrs.add(address);
                count++;
                decrypted = decrypted.attach(key.decrypt(address));
            }

            if (addrs.size() != count || count != expected) {
                phase.set(Phase.Blame);
                mailbox.broadcast(messages.make().attach(Blame.ShuffleFailure(players.get(N))),
                        phase.get());

                return null;
            }

            return decrypted;
        }

        // Some misbehavior that has occurred during the shuffle phase and we want to
        // find out what happened!
        private void blameShuffleMisbehavior()
                throws WaitingException, Matrix,
                InterruptedException, FormatException, IOException {

            // First skip to phase 4 and do an equivocation check.
            phase.set(Phase.EquivocationCheck);
            equivocationCheck(encryptionKeys, newAddresses, true);
        }

        // Players run an equivocation check when they must confirm that they all have
        // the same information.
        void equivocationCheck(
                Map<VerificationKey,
                EncryptionKey> encryptonKeys,
                Queue<Address> newAddresses,
                boolean errorCase // There is an equivocation check that occurs
        ) throws InterruptedException, WaitingException, Matrix, IOException, FormatException {

            Message equivocationCheck = equivocationCheckHash(players, encryptonKeys, newAddresses);
            mailbox.broadcast(equivocationCheck, phase.get());

            // Wait for a similar message from everyone else and check that the result is the name.
            Map<VerificationKey, Message> hashes = null;
            hashes = mailbox.receiveFromMultipleBlameless(playerSet(1, players.size()),
                    phase.get());

            hashes.put(vk, equivocationCheck);

            if (areEqual(hashes.values())) {
                // We may have got this far as part of a normal part of the protocol or as a part
                // of an error case. If this is a normal part of the protocol, a blame message
                // having been received indicates that another player has a problem with the
                // output vector received from the last player in phase 3.
                if (errorCase
                        || mailbox.blame(Reason.ShuffleFailure)
                        || mailbox.blame(Reason.MissingOutput)) {
                    blameBroadcastShuffleMessages();
                }

                return;
            }

            log.warn("Player " + me + " equivocation check fails.");

            // If the hashes are not equal, enter the blame phase.
            // Collect all packets from phase 1 and 3.
            phase.set(Phase.Blame);
            Queue<Packet> evidence = mailbox.getPacketsByPhase(Phase.Announcement);
            evidence.addAll(mailbox.getPacketsByPhase(Phase.BroadcastOutput));
            mailbox.broadcast(messages.make().attach(Blame.EquivocationFailure(evidence)),
                    phase.get());

            throw fillBlameMatrix();
        }

        // Check for players with insufficient funds.
        private void blameInsufficientFunds()
                throws CoinNetworkException, WaitingException, Matrix, IOException, InterruptedException, FormatException {
            List<VerificationKey> offenders = new LinkedList<>();

            // Check that each participant has the required amounts.
            for (VerificationKey player : players.values()) {
                if (coin.valueHeld(player.address()) < amount) {
                    // Enter the blame phase.
                    offenders.add(player);
                }
            }

            // If they do, return.
            if (offenders.isEmpty()) return;

            // If not, enter blame phase and find offending transactions.
            phase.set(Phase.Blame);
            Message blameMessage = messages.make();
            for (VerificationKey offender : offenders) {
                Transaction t = coin.getConflictingTransaction(offender.address(), amount);

                if (t == null) {
                    blameMessage = blameMessage.attach(Blame.NoFundsAtAll(offender));
                } else {
                    blameMessage = blameMessage.attach(Blame.InsufficientFunds(offender, t));
                }
            }

            // Broadcast offending transactions.
            mailbox.broadcast(blameMessage, phase.get());

            // Get all subsequent blame messages.
            throw fillBlameMatrix();
        }

        void blameBroadcastShuffleMessages()
                throws WaitingException, Matrix, IOException,
                InterruptedException, FormatException {

            phase.set(Phase.Blame);
            log.warn("Player " + me + " enters blame phase and sends broadcast messages.");

            // Collect all packets from phase 2 and 3.
            Queue<Packet> evidence = mailbox.getPacketsByPhase(Phase.Shuffling);
            evidence.addAll(mailbox.getPacketsByPhase(Phase.BroadcastOutput));

            // Send them all with the decryption key.
            mailbox.broadcast(messages.make().attach(
                    Blame.ShuffleAndEquivocationFailure(dk, evidence)), phase.get());

            throw fillBlameMatrix();
        }

        void checkDoubleSpending(Transaction t) throws InterruptedException, IOException,
                FormatException, WaitingException, Matrix {

            // Check for double spending.
            Message doubleSpend = messages.make();
            for (VerificationKey key : players.values()) {
                Transaction o = coin.getConflictingTransaction(key.address(), amount);
                if (o != null) {
                    doubleSpend = doubleSpend.attach(Blame.DoubleSpend(key, o));
                }
            }

            if (!doubleSpend.isEmpty()) {
                phase.set(Phase.Blame);

                mailbox.broadcast(doubleSpend, phase.get());
                throw fillBlameMatrix();
            }
        }

        // When we know we'll receive a bunch of blame messages, we have to go through them all
        // to figure out what's going on.
        final Matrix fillBlameMatrix() throws IOException, InterruptedException, FormatException {
            Matrix matrix = new Matrix();

            Map<VerificationKey, Queue<Packet>> blameMessages = mailbox.receiveAllBlame();

            // Get all hashes received in phase 4 to check that they were reported correctly.
            Map<VerificationKey, Message> hashes = new HashMap<>();
            {
                Queue<Packet> hashMessages =
                        mailbox.getPacketsByPhase(Phase.EquivocationCheck);

                for (Packet packet : hashMessages) {
                    hashes.put(packet.from(), packet.payload());
                    // Include my own hash.
                    hashes.put(vk, equivocationCheckHash(players, encryptionKeys, newAddresses));
                }
            }

            // The messages sent in the broadcast phase by the last player to all the other players.
            Map<VerificationKey, Packet> outputVectors = new HashMap<>();

            // The encryption keys sent from every player to every other.
            Map<VerificationKey, Map<VerificationKey, EncryptionKey>> sentKeys = new HashMap<>();

            // The list of messages history in phase 2.
            Map<VerificationKey, Packet> shuffleMessages = new HashMap<>();

            // The set of decryption keys from each player.
            Map<VerificationKey, DecryptionKey> decryptionKeys = new HashMap<>();

            // Determine who is being blamed and by whom.
            for (Map.Entry<VerificationKey, Queue<Packet>> entry : blameMessages.entrySet()) {
                VerificationKey from = entry.getKey();
                Queue<Packet> responses = entry.getValue();
                for (Packet packet : responses) {
                    Message message = packet.payload();

                    if (message.isEmpty()) {
                        log.error("Empty blame message received from " + from);
                        matrix.put(vk, Evidence.Placeholder(from, Reason.EmptyBlameMessage));
                    }

                    while (!message.isEmpty()) {
                        Blame blame = message.readBlame();
                        message = message.rest();

                        switch (blame.reason) {
                            case NoFundsAtAll: {
                                // TODO Do we agree that this player has insufficient funds?
                                matrix.put(from, Evidence.NoFundsAtAll(blame.accused));
                                break;
                            }
                            case InsufficientFunds: {
                                if (blame.t == null) {
                                    // A transaction must be included to claim insufficient funds.
                                    matrix.put(vk, Evidence.Placeholder(from, Reason.Liar));
                                    break;
                                }

                                // Is the evidence included sufficient?
                                // TODO check whether this is a conflicting transaction.
                                matrix.put(from, Evidence.InsufficientFunds(blame.accused, blame.t));
                                break;
                            }
                            case EquivocationFailure: {
                                // These are the keys received by
                                // this player in the announcement phase.
                                Map<VerificationKey, EncryptionKey> receivedKeys = new HashMap<>();
                                fillBlameMatrixCollectHistory(vk, from, blame.packets, matrix,
                                        outputVectors, shuffleMessages, receivedKeys, sentKeys);

                                Queue<Address> addresses = new LinkedList<>();

                                // The last player will not have
                                // received a separate set of addresses for
                                // us to check, so we insert our own.
                                if (from.equals(players.get(N))) {
                                    addresses = newAddresses;
                                } else {

                                    Message output = outputVectors.get(from).payload();
                                    while (!output.isEmpty()) {
                                        addresses.add(output.readAddress());
                                        output = output.rest();
                                    }
                                }

                                // If the sender is not player one, we add the keys he sent us,
                                // as he would not have received any set of keys from himself.
                                if (!from.equals(players.get(1))) {
                                    receivedKeys.put(from, encryptionKeys.get(from));
                                }

                                // Check if this player correctly reported
                                // the hash previously sent to us.
                                if (!hashes.get(from).equals(
                                        equivocationCheckHash(players, receivedKeys, addresses))
                                        ) {
                                    matrix.put(vk, Evidence.Placeholder(from, Reason.Liar));
                                }

                                break;
                            }
                            case ShuffleAndEquivocationFailure: {
                                if (decryptionKeys.containsKey(from)) {
                                    // TODO blame someone here.
                                }
                                decryptionKeys.put(from, blame.privateKey);

                                // Check that the decryption key is valid. (The decryption key
                                // can be null for player 1, who doesn't make one.)
                                if (packet.from() != players.get(1)) {
                                    if (blame.privateKey == null)  {
                                        // TODO blame someone here.
                                    } else if (
                                            !blame.privateKey.EncryptionKey(
                                            ).equals(encryptionKeys.get(from))
                                            ) {
                                        // TODO blame someone here.
                                    }
                                }

                                fillBlameMatrixCollectHistory(vk, from, blame.packets, matrix,
                                        outputVectors, shuffleMessages,
                                        new HashMap<VerificationKey, EncryptionKey>(), sentKeys);

                                break;
                            }
                            case DoubleSpend: {
                                // TODO does this actually spend the funds?
                                matrix.put(from, Evidence.DoubleSpend(blame.accused, blame.t));
                                break;
                            }
                            case InvalidSignature: {
                                if (blame.invalid == null || blame.accused == null) {
                                    matrix.put(vk, Evidence.Placeholder(from, Reason.Liar));
                                    break;
                                }

                                // TODO do we agree that the signature is invalid?
                                matrix.put(from,
                                        Evidence.InvalidSignature(blame.accused, blame.invalid));

                                break;
                            }
                            // These should already have been handled.
                            case MissingOutput: {
                                break;
                            }
                            case ShuffleFailure: {
                                break;
                            }
                            default:
                                throw new InvalidImplementationError();
                        }
                    }
                }
            }

            // Check that we have all the required announcement messages.
            if (sentKeys.size() > 0) {
                for (int i = 2; i < players.size(); i++) {
                    if (i == me) {
                        continue;
                    }

                    VerificationKey from = players.get(i);

                    Map<VerificationKey, EncryptionKey> sent = sentKeys.get(from);

                    if (sent == null) {
                        // This should not really happen.
                        continue;
                    }

                    // Add in the key I received from this player.
                    sent.put(vk, encryptionKeys.get(from));

                    EncryptionKey key = null;
                    for (int j = 1; j <= players.size(); j++) {
                        if (i == j) {
                            continue;
                        }

                        VerificationKey to = players.get(j);

                        EncryptionKey next = sent.get(to);

                        if (next == null) {
                            // blame player to. He should have sent us this.

                            matrix.put(vk, Evidence.Placeholder(to, Reason.Liar));
                            continue;
                        }

                        if (key != null && !key.equals(next)) {
                            matrix.put(vk, Evidence.EquivocationFailureAnnouncement(from, sent));
                            break;
                        }

                        key = next;
                    }
                }
            }

            boolean outputEquivocate = false;
            if (outputVectors.size() > 0) {
                // Add our own vector to this.
                if (me != N) {
                    outputVectors.put(vk, mailbox.getPacketsByPhase(Phase.BroadcastOutput).peek());
                }

                // We should have one output vector for every player except the last and ourselves.
                Set<VerificationKey> leftover = playerSet(1, N - 1);
                leftover.removeAll(outputVectors.keySet());
                if (leftover.size() > 0) {
                    for (VerificationKey key : leftover) {
                        matrix.put(vk, Evidence.Placeholder(key, Reason.Liar));
                    }
                }

                List<Message> outputMessages = new LinkedList<>();
                for (Packet packet : outputVectors.values()) {
                    outputMessages.add(packet.payload());
                }

                // If they are not all equal, blame the last player for equivocating.
                if (!areEqual(outputMessages)) {
                    matrix.put(vk,
                            Evidence.EquivocationFailureBroadcast(players.get(N), outputVectors));
                    outputEquivocate = true;
                }
            }

            if (decryptionKeys.size() > 0) {

                // We should have one decryption key for every player except the first.
                Set<VerificationKey> leftover = playerSet(2, players.size());
                leftover.removeAll(decryptionKeys.keySet());
                if (leftover.size() > 0) {
                    log.warn("leftover");
                    // TODO blame someone.
                } else {
                    Evidence shuffleEvidence = checkShuffleMisbehavior(players, decryptionKeys,
                            shuffleMessages, outputEquivocate ? null : outputVectors);
                    if (shuffleEvidence != null) {
                        matrix.put(vk, shuffleEvidence);
                    }
                }
            }

            return matrix;
        }

        // Get the set of players from i to N.
        public final Set<VerificationKey> playerSet(int i, int n)
                throws InvalidImplementationError {

            if (i < 1) {
                i = 1;
            }
            Set<VerificationKey> set = new HashSet<>();
            for(int j = i; j <= n; j ++) {
                if (j > N) {
                    return set;
                }

                set.add(players.get(j));
            }

            return set;
        }

        // get the set of all players for this round.
        public final Set<VerificationKey> playerSet() {
            return playerSet(1, N);
        }

        // Generate the message sent during the equivocation check phase.
        // This message hashes some information that the player has received.
        // It is used to check that other players have received the same information.
        final Message equivocationCheckHash(
                Map<Integer, VerificationKey> players,
                Map<VerificationKey, EncryptionKey> encryptionKeys,
                Queue<Address> newAddresses) {

            // Put all temporary encryption keys into a list and hash the result.
            Message check = messages.make();
            for (int i = 2; i <= players.size(); i++) {
                check = check.attach(encryptionKeys.get(players.get(i)));
            }

            // During a normal round of the protocol, the players have all received a
            // new set of addresses by this point and those also need to be included. However,
            // there is also an error case in which we don't have the new addresses yet, in which
            // case, they are not included.
            if (newAddresses != null) {
                for (Address address : newAddresses) {
                    check = check.attach(address);
                }
            }

            return check.hashed();
        }

        // A round is a single run of the protocol.
        Round(  CurrentPhase phase,
                long amount,
                SigningKey sk,
                Map<Integer, VerificationKey> players,
                Address addrNew,
                Address change,
                Mailbox mailbox) throws InvalidParticipantSetException {

            this.phase = phase;
            this.amount = amount;
            this.sk = sk;
            this.players = players;
            this.change = change;
            vk = sk.VerificationKey();
            this.mailbox = mailbox;
            this.addrNew = addrNew;

            int m = -1;
            N = players.size();

            // Determine what my index number is.
            for (int i = 1; i <= N; i++) {
                if (players.get(i).equals(vk)) {
                    m = i;
                    break;
                }
            }
            me = m;

            if (me < 0) {
                throw new InvalidParticipantSetException(vk, players);
            }
        }
    }

    // Algorithm to randomly shuffle the elements of a message.
    final Message shuffle(Message message) throws InvalidImplementationError, FormatException {

        Message shuffled = messages.make();

        // Read all elements of the packet and insert them in a Queue.
        Queue<Address> old = new LinkedList<>();
        int N = 0;
        while (!message.isEmpty()) {
            old.add(message.readAddress());
            message = message.rest();
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
            shuffled = shuffled.attach(old.remove());
        }

        return shuffled;
    }

    // Test whether a set of messages are equal.
    static boolean areEqual(Iterable<Message> messages) throws InvalidImplementationError {
        Message last = null;
        for (Message m : messages) {
            if (last != null) {
                boolean equal = last.equals(m);
                if (!equal) {
                    return false;
                }
            }

            last = m;
        }

        return true;
    }

    // In phase 1, everybody announces their new encryption keys to one another. They also
    // optionally send change addresses to one another. This function reads that information
    // from a message and puts it in some nice data structures.
    private static void readAnnouncements(Map<VerificationKey, Message> messages,
                           Map<VerificationKey, EncryptionKey> encryptionKeys,
                           Map<VerificationKey, Address> change) throws FormatException {
        for (Map.Entry<VerificationKey, Message> entry : messages.entrySet()) {
            VerificationKey key = entry.getKey();
            Message message = entry.getValue();

            encryptionKeys.put(key, message.readEncryptionKey());

            message = message.rest();
            if (!message.isEmpty()) {
                change.put(key, message.readAddress());
            }
        }
    }

    // This function is only called by fillBlameMatrix to collect messages sent in
    // phases 1, 2, and 3. and to organize the information appropriately.
    private static void fillBlameMatrixCollectHistory(
            VerificationKey vk,
            VerificationKey from,
            Queue<Packet> packets,
            Matrix matrix,
            // The messages sent in the broadcast phase by the last player to all the other players.
            Map<VerificationKey, Packet> outputVectors,
             // The list of messages history in phase 2.
            Map<VerificationKey, Packet> shuffleMessages,
            // The keys received by everyone in the announcement phase.
            Map<VerificationKey, EncryptionKey> receivedKeys,
            // The keys sent by everyone in the announcement phase.
            Map<VerificationKey, Map<VerificationKey, EncryptionKey>> sentKeys
    ) throws FormatException {

        if (packets == null) {
            log.error("Player " + vk.toString() + " null blames " + from.toString() + ", case J");
            matrix.put(vk, Evidence.Placeholder(from, Reason.Liar));
            return;
        }

        // Collect all packets received in the appropriate place.
        for (Packet packet : packets) {
            switch (packet.phase()) {
                case BroadcastOutput: {
                    if (outputVectors.containsKey(from)) {
                        // We should only ever receive one such message from each player.
                        if (outputVectors.containsKey(from)
                                && !outputVectors.get(from).equals(packet)) {

                            log.error("Player " + vk.toString() + " null blames " + from.toString()
                                    + ", case A; " + outputVectors.get(from).toString() + " != "
                                    + packet.toString());
                            matrix.put(vk, Evidence.Placeholder(from, Reason.Liar));
                        }
                    }
                    outputVectors.put(from, packet);
                    break;
                }
                case Announcement: {
                    Map<VerificationKey, EncryptionKey> map = sentKeys.get(packet.from());
                    if (map == null) {
                        map = new HashMap<>();
                        sentKeys.put(packet.from(), map);
                    }

                    EncryptionKey key = packet.payload().readEncryptionKey();
                    map.put(from, key);
                    receivedKeys.put(packet.from(), key);
                    break;
                }
                case Shuffling: {
                    if (shuffleMessages.containsKey(from)) {
                        // TODO blame someone here.
                    }
                    shuffleMessages.put(from, packet);
                    break;
                }
                default: {
                    // This case should never happen.
                    // It's not malicious but it's not allowed either.
                    log.error("Player " + vk.toString()
                            + " null blames " + from.toString() + ", case C");
                    matrix.put(vk, Evidence.Placeholder(from, Reason.Liar));
                    break;
                }
            }
        }
    }

    private static Evidence checkShuffleMisbehavior(
            Map<Integer, VerificationKey> players,
            Map<VerificationKey, DecryptionKey> decryptionKeys,
            Map<VerificationKey, Packet> shuffleMessages,
            Map<VerificationKey, Packet> broadcastMessages) throws FormatException {

        if (players == null || decryptionKeys == null
                || shuffleMessages == null || broadcastMessages == null)
            throw new NullPointerException();

        SortedSet<Address> outputs = new TreeSet<>();

        // Go through the steps of shuffling messages.
        for (int i = 1; i <= players.size(); i++) {

            // The last step is from phase three, so we have to check for that.
            Packet packet = null;
            if (i < players.size()) {
                packet = shuffleMessages.get(players.get(i + 1));
            } else {
                // All broadcast messages should have the same content and we should
                // have already checked for this. Therefore we just look for the first
                // one that is available. (The message for player 1 should always be available.)
                for (int j = 1; j <= players.size(); j++) {
                    packet = broadcastMessages.get(players.get(j));

                    if (packet != null) {
                        break;
                    }
                }
            }

            if (packet == null) {
                // TODO Blame a player for lying.

                return null;
            }

            Message message = packet.payload();

            // Grab the correct number of addresses and decrypt them.
            SortedSet<Address> addresses = new TreeSet<>();
            for (int j = 0; j < i; j++) {
                if (message.isEmpty()) {
                    return Evidence.ShuffleMisbehaviorDropAddress(
                            players.get(i), decryptionKeys, shuffleMessages, broadcastMessages);
                }

                Address address = message.readAddress();
                message = message.rest();
                for (int k = i + 1; k <= players.size(); k++) {
                    address = decryptionKeys.get(players.get(k)).decrypt(address);
                }

                // There shouldn't be duplicates.
                if (addresses.contains(address)) {
                    return Evidence.ShuffleMisbehaviorDropAddress(
                            players.get(i), decryptionKeys, shuffleMessages, broadcastMessages);
                }
                addresses.add(address);
            }

            // Does this contain all the previous addresses?
            if (!addresses.containsAll(outputs)) {
                return Evidence.ShuffleMisbehaviorDropAddress(
                        players.get(i), decryptionKeys, shuffleMessages, broadcastMessages);
            }

            addresses.removeAll(outputs);

            // There should be one new address.
            if (addresses.size() != 1) {
                return Evidence.ShuffleMisbehaviorDropAddress(
                        players.get(i), decryptionKeys, shuffleMessages, broadcastMessages);
            }

            outputs.add(addresses.first());
        }

        return null;
    }

    // Run the protocol without creating a new thread.
    public Transaction runProtocol(
            long amount, // The amount to be shuffled per player.
            SigningKey sk, // The signing key of the current player.
            // The set of players, sorted alphabetically by address.
            SortedSet<VerificationKey> players,
            Address addrNew, // My new (anonymous) address.
            Address change, // Change address. (can be null)
            // If this is not null, the machine is put in this channel so that another thread can
            // query the phase as it runs.
            Send<Phase> chan
    ) throws WaitingException, Matrix, InterruptedException, InvalidParticipantSetException,
            FormatException, IOException, CoinNetworkException {

        if (amount <= 0) {
            throw new IllegalArgumentException();
        }
        if (sk == null || players == null) {
            throw new NullPointerException();
        }

        CurrentPhase machine;
        if (chan == null) {
            machine = new CurrentPhase();
        } else {
            machine = new CurrentPhase(chan);
        }

        // Get the initial ordering of the players.
        int i = 1;
        Map<Integer, VerificationKey> numberedPlayers = new TreeMap<>();
        for (VerificationKey player : players) {
            numberedPlayers.put(i, player);
            i++;
        }

        // Make an inbox for the next round.
        Mailbox mailbox = new Mailbox(
                sk.VerificationKey(), numberedPlayers.values(), messages);

        return this.new Round(
                machine, amount, sk, numberedPlayers, addrNew, change, mailbox
        ).protocolDefinition();
    }

    public CoinShuffle(
            MessageFactory messages, // Object that knows how to create and copy messages.
            Crypto crypto, // Connects to the cryptography.
            Coin coin // Connects us to the Bitcoin or other cryptocurrency netork.
    ) {
        if (crypto == null || coin == null || messages == null) {
            throw new NullPointerException();
        }
        this.crypto = crypto;
        this.coin = coin;
        this.messages = messages;
    }

    /**
     * The current phase of the protocol. Can be monitored from another thread.
     *
     * Created by Daniel Krawisz on 2/8/16.
     */
    static class CurrentPhase {

        private final Send<Phase> ch;
        private Phase phase = Phase.Uninitiated;

        // the phase can be accessed concurrently in case we want to update
        // the user on how the protocol is going.
        public Phase get() {
            return phase;
        }

        public void set(Phase phase) throws InterruptedException {
            this.phase = phase;
            if (ch != null) {
                ch.send(phase);
            }
        }

        public CurrentPhase() {
            ch = null;
        }

        public CurrentPhase(Send<Phase> ch) throws InterruptedException {
            if (ch == null) {
                throw new NullPointerException();
            }

            this.ch = ch;
            ch.send(Phase.Uninitiated);
        }
    }
}
