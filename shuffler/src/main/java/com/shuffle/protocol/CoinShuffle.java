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
import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.bitcoin.Signature;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.Chan;
import com.shuffle.chan.SendChan;
import com.shuffle.protocol.blame.Blame;
import com.shuffle.protocol.blame.BlameException;
import com.shuffle.protocol.blame.Matrix;
import com.shuffle.protocol.blame.Evidence;
import com.shuffle.protocol.blame.Reason;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.ProtocolException;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    static Logger log= LogManager.getLogger(CoinShuffle.class);

    final Crypto crypto;

    final Coin coin;

    final MessageFactory messages;

    // A single round of the protocol. It is possible that the players may go through
    // several failed rounds until they have eliminated malicious players.
    class Round {
        final Machine machine;

        final SessionIdentifier session;

        final long amount; // The amount to be shuffled.

        final SigningKey sk; // My signing private key.

        final public int me; // Which player am I?

        final public Map<Integer, VerificationKey> players; // The keys representing all the players.

        final public int N; // The number of players.

        final public VerificationKey vk; // My verification public key, which is also my identity.

        public DecryptionKey dk = null;

        // This will contain the new encryption public keys.
        final public Map<VerificationKey, EncryptionKey> encryptionKeys = new HashMap<>();

        // The set of new addresses into which the coins will be deposited.
        public Queue<Address> newAddresses = null;

        final public Address change; // My change address. (may be null).

        final public Map<VerificationKey, Signature> signatures = new HashMap<>();

        final public Mailbox mailbox;

        void protocolDefinition(
        ) throws
                TimeoutError,
                FormatException,
                CryptographyError,
                InvalidImplementationError,
                ValueException,
                InterruptedException,
                SignatureException,
                ProtocolException,
                CoinNetworkException {

            if (amount <= 0) {
                throw new IllegalArgumentException();
            }

            // Phase 1: Announcement
            // In the announcement phase, participants distribute temporary encryption keys.
            machine.phase = Phase.Announcement;
            log.info("Player " + me + " begins Coin Shuffle protocol " + session + " with " + N + " players.");

            // Check for sufficient funds.
            // There was a problem with the wording of the original paper which would have meant
            // that player 1's funds never would have been checked, but we have to do that.
            machine.matrix = blameInsufficientFunds();
            if (machine.matrix != null) {
                return;
            }

            // This will contain the change addresses.
            Map<VerificationKey, Address> changeAddresses = new HashMap<>();

            // Everyone except player 1 creates a new keypair and sends it around to everyone else.
            dk = newDecryptionKey(changeAddresses);

            // Now we wait to receive similar key from everyone else.
            Map<VerificationKey, Message> announcement;
            try {
                announcement = mailbox.receiveFromMultiple(playerSet(2, N), machine.phase);
            } catch (BlameException e) {
                // might receive blame messages about insufficient funds.
                machine.phase = Phase.Blame;
                machine.matrix = fillBlameMatrix(new Matrix());
                return;
            }

            readAnnouncements(announcement, encryptionKeys, changeAddresses);

            // Phase 2: Shuffle
            // In the shuffle phase, we create a sequence of orderings which will b successively
            // applied by each particpant. Everyone has the incentive to insert their own address
            // at a random location, which is sufficient to ensure randomness of the whole thing
            // to all participants.
            machine.phase = Phase.Shuffling;

            // Each participant chooses a new bitcoin address which will be their new outputs.
            SigningKey sk_new = crypto.makeSigningKey();
            Address addr_new = sk_new.VerificationKey().address();

            try {
                // Player one begins the cycle and encrypts its new address with everyone's privateKey, in order.
                // Each subsequent player reorders the cycle and removes one layer of encryption.
                Message shuffled = messages.make();
                if (me != 1) {
                    shuffled = decryptAll(shuffled.attach(mailbox.receiveFrom(players.get(me - 1), machine.phase)), dk, me - 1);
                    if (shuffled == null) {
                        machine.matrix = blameShuffleMisbehavior();
                        return;
                    }
                }

                // Make an encrypted address to the mix, and then shuffle everything ourselves.
                shuffled = shufflePhase(shuffled, addr_new);

                // Pass it along to the next player.
                if (me != N) {
                    mailbox.send(new Packet(shuffled, session, machine.phase, vk, players.get(me + 1)));
                }

                // Phase 3: broadcast outputs.
                // In this phase, the last player just broadcasts the transaction to everyone else.
                machine.phase = Phase.BroadcastOutput;

                newAddresses = readAndBroadcastNewAddresses(shuffled);
            } catch (BlameException e) { // Any blame message we receive in this phase should be
                                        // dealt with immediately because this error case does
                                       // not use the new addresses.
                switch (e.packet.message.readBlame().reason) {
                    case ShuffleFailure:
                    case MissingOutput: {
                        machine.matrix = blameShuffleMisbehavior();
                        return;
                    }
                    default: {
                        machine.matrix = fillBlameMatrix(new Matrix());
                        return;
                    }
                }
            }

            // Everyone else receives the broadcast and checks to make sure their message was included.
            if (!newAddresses.contains(addr_new)) {
                machine.phase = Phase.Blame;
                mailbox.broadcast(messages.make().attach(Blame.MissingOutput(players.get(N))), machine.phase);
                machine.matrix = blameShuffleMisbehavior();
                return;
            }

            // Phase 4: equivocation check.
            // In this phase, participants check whether any player has history different
            // encryption keys to different players.
            machine.phase = Phase.EquivocationCheck;

            machine.matrix = equivocationCheck(encryptionKeys, newAddresses, false);
            if (machine.matrix != null) {
                return;
            }

            // Phase 5: verification and submission.
            // Everyone creates a Bitcoin transaction and signs it, then broadcasts the signature.
            // If all signatures check out, then the transaction is history into the net.
            machine.phase = Phase.VerificationAndSubmission;

            List<VerificationKey> inputs = new LinkedList<>();
            for (int i = 1; i <= N; i++) {
                inputs.add(players.get(i));
            }

            machine.t = coin.shuffleTransaction(amount, inputs, newAddresses, changeAddresses);

            machine.matrix = checkDoubleSpending(machine.t);
            if (machine.matrix != null) {
                return;
            }

            mailbox.broadcast(messages.make().attach(sk.makeSignature(machine.t)), machine.phase);

            Map<VerificationKey, Message> signatureMessages = null;
            try {
                signatureMessages = mailbox.receiveFromMultiple(playerSet(1, N), machine.phase);
            } catch (BlameException e) {
                // Could receive notice of double spending here.
                machine.matrix = fillBlameMatrix(new Matrix());
                return;
            }

            // Verify the signatures.
            assert signatureMessages != null;
            Map<VerificationKey, Signature> invalid = new HashMap<>();
            for (Map.Entry<VerificationKey, Message> sig : signatureMessages.entrySet()) {
                VerificationKey key = sig.getKey();
                Signature signature = sig.getValue().readSignature();
                signatures.put(key, signature);
                if (!key.verify(machine.t, signature)) {
                    invalid.put(key, signature);
                }
            }

            if (invalid.size() > 0) {
                machine.phase = Phase.Blame;
                Matrix bm = new Matrix();
                Message blameMessage = messages.make();

                for(Map.Entry<VerificationKey, Signature> bad : invalid.entrySet()) {
                    VerificationKey key = bad.getKey();
                    Signature signature = bad.getValue();

                    bm.put(vk, Evidence.InvalidSignature(key, signature));
                    blameMessage.attach(Blame.InvalidSignature(key, signature));
                }
                mailbox.broadcast(blameMessage, machine.phase);
                machine.matrix = fillBlameMatrix(bm);
                return;
            }

            // Send the transaction into the net.
            try {
                machine.t.send();
            } catch(CoinNetworkException e) {
                machine.e = e;
            }

            // The protocol has completed successfully.
            machine.phase = Phase.Completed;
        }

        // Everyone except player 1 creates a new keypair and sends it around to everyone else.
        DecryptionKey newDecryptionKey(Map<VerificationKey, Address> changeAddresses) {
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
                mailbox.broadcast(message, machine.phase);
            }
            return dk;
        }

        Deque<Address> readNewAddresses(Message message) throws FormatException, InvalidImplementationError {
            Deque<Address> queue = new LinkedList<>();

            while (!message.isEmpty()) {
                queue.add(message.readAddress());
                message = message.rest();
            }

            return queue;
        }

        // The shuffle phase.
        Message shufflePhase(Message shuffled, Address addr_new)
                throws InterruptedException, BlameException,
                SignatureException, ValueException, FormatException {

            // Add our own address to the mix. Note that if me == N, ie, the last player, then no
            // encryption is done. That is because we have reached the last layer of encryption.
            Address encrypted = addr_new;
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
                throws FormatException, InterruptedException,
                SignatureException, ValueException, BlameException {
            Deque<Address> newAddresses;
            if (me == N) {
                // The last player adds his own new address in without encrypting anything and shuffles the result.
                newAddresses = readNewAddresses(shuffled);
                mailbox.broadcast(shuffled, machine.phase);
            } else {
                newAddresses = readNewAddresses(mailbox.receiveFrom(players.get(N), machine.phase));
            }

            return newAddresses;
        }

        // In the shuffle phase, we have to receive a set of strings from the previous player and
        // decrypt them all.
        final Message decryptAll(Message message, DecryptionKey key, int expected) throws InvalidImplementationError, FormatException {
            Message decrypted = messages.make();

            int count = 0;
            Set<Address> addrs = new HashSet<>(); // Used to check that all addresses are different.

            while (!message.isEmpty()) {
                Address address = message.readAddress();
                message = message.rest();

                addrs.add(address);
                count++;
                try {
                    decrypted = decrypted.attach(key.decrypt(address));
                } catch (CryptographyError e) {
                    mailbox.broadcast(messages.make().attach(Blame.ShuffleFailure()), machine.phase);
                    return null;
                }
            }

            if (addrs.size() != count || count != expected) {
                machine.phase = Phase.Blame;
                mailbox.broadcast(messages.make().attach(Blame.MissingOutput(players.get(N))), machine.phase);
                return null;
            }

            return decrypted;
        }

        // Players run an equivocation check when they must confirm that they all have
        // the same information.
        Matrix equivocationCheck(
                Map<VerificationKey, EncryptionKey> encryptonKeys,
                Queue<Address> newAddresses, boolean errorCase)
                throws InterruptedException, ValueException,
                FormatException, ProtocolException,
                SignatureException {

            Message equivocationCheck = equivocationCheckHash(players, encryptonKeys, newAddresses);
            mailbox.broadcast(equivocationCheck, machine.phase);

            // Wait for a similar message from everyone else and check that the result is the name.
            Map<VerificationKey, Message> hashes = null;
            hashes = mailbox.receiveFromMultipleBlameless(playerSet(1, players.size()), machine.phase);
            hashes.put(vk, equivocationCheck);

            if (areEqual(hashes.values())) {
                // We may have got this far as part of a normal part of the protocol or as a part
                // of an error case. If this is a normal part of the protocol, a blame message
                // having been received indicates that another player has a problem with the
                // output vector received from the last player in phase 3.
                if (mailbox.blame() || errorCase) {
                    return blameBroadcastShuffleMessages();
                }

                return null;
            }

            log.warn("Player " + me + " equivocation check fails.");

            // If the hashes are not equal, enter the blame phase.
            // Collect all packets from phase 1 and 3.
            machine.phase = Phase.Blame;
            Queue<SignedPacket> evidence = mailbox.getPacketsByPhase(Phase.Announcement);
            evidence.addAll(mailbox.getPacketsByPhase(Phase.BroadcastOutput));
            mailbox.broadcast(messages.make().attach(Blame.EquivocationFailure(evidence)), machine.phase);

            return fillBlameMatrix(new Matrix());
        }

        // Check for players with insufficient funds.
        private Matrix blameInsufficientFunds() throws
                InterruptedException, FormatException,
                ValueException, SignatureException, CoinNetworkException
        {
            List<VerificationKey> offenders = new LinkedList<>();

            // Check that each participant has the required amounts.
            for (VerificationKey player : players.values()) {
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
            machine.phase = Phase.Blame;
            Matrix matrix = new Matrix();
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
            mailbox.broadcast(blameMessage, machine.phase);

            // Get all subsequent blame messages.
            return fillBlameMatrix(matrix);
        }

        // Some misbehavior that has occurred during the shuffle phase and we want to
        // find out what happened!
        private Matrix blameShuffleMisbehavior()
                throws InterruptedException,
                FormatException,
                ValueException,
                ProtocolException,
                SignatureException {

            // First skip to phase 4 and do an equivocation check.
            machine.phase = Phase.EquivocationCheck;
            return equivocationCheck(encryptionKeys, newAddresses, true);
        }

        protected Matrix blameBroadcastShuffleMessages() throws InterruptedException, SignatureException, ValueException, FormatException {
            machine.phase = Phase.Blame;
            log.warn("Player " + me + " enters blame phase and sends broadcast messages.");

            // Collect all packets from phase 2 and 3.
            Queue<SignedPacket> evidence = mailbox.getPacketsByPhase(Phase.Shuffling);
            evidence.addAll(mailbox.getPacketsByPhase(Phase.BroadcastOutput));

            // Send them all with the decryption key.
            mailbox.broadcast(messages.make().attach(Blame.ShuffleAndEquivocationFailure(dk, evidence)), machine.phase);

            return fillBlameMatrix(new Matrix());
        }

        protected Matrix checkDoubleSpending(Transaction t) throws
                InterruptedException, SignatureException, ValueException, FormatException {
            // Check for double spending.
            Message doubleSpend = messages.make();
            for (VerificationKey key : players.values()) {
                Transaction o = coin.getConflictingTransaction(key.address(), amount);
                if (o != null) {
                    doubleSpend = doubleSpend.attach(Blame.DoubleSpend(key, o));
                }
            }

            if (!doubleSpend.isEmpty()) {
                machine.phase = Phase.Blame;

                mailbox.broadcast(doubleSpend, machine.phase);
                return fillBlameMatrix(new Matrix());
            }

            return null;
        }

        // When we know we'll receive a bunch of blame messages, we have to go through them all to figure
        // out what's going on.
        final Matrix fillBlameMatrix(Matrix matrix) throws
                InterruptedException,
                FormatException,
                ValueException,
                SignatureException {
            Map<VerificationKey, Queue<SignedPacket>> blameMessages = mailbox.receiveAllBlame();

            // Get all hashes received in phase 4 so that we can check that they were reported correctly.
            Map<VerificationKey, Message> hashes = new HashMap<>();
            {
                Queue<SignedPacket> hashMessages = mailbox.getPacketsByPhase(Phase.EquivocationCheck);
                for (SignedPacket packet : hashMessages) {
                    hashes.put(packet.payload.signer, packet.payload.message);
                    // Include my own hash.
                    hashes.put(vk, equivocationCheckHash(players, encryptionKeys, newAddresses));
                }
            }

            // The messages sent in the broadcast phase by the last player to all the other players.
            Map<VerificationKey, SignedPacket> outputVectors = new HashMap<>();

            // The encryption keys sent from every player to every other.
            Map<VerificationKey, Map<VerificationKey, EncryptionKey>> sentKeys = new HashMap<>();

            // The list of messages history in phase 2.
            Map<VerificationKey, SignedPacket> shuffleMessages = new HashMap<>();

            // The set of decryption keys from each player.
            Map<VerificationKey, DecryptionKey> decryptionKeys = new HashMap<>();

            // Determine who is being blamed and by whom.
            for (Map.Entry<VerificationKey, Queue<SignedPacket>> entry : blameMessages.entrySet()) {
                VerificationKey from = entry.getKey();
                Queue<SignedPacket> responses = entry.getValue();
                for (SignedPacket packet : responses) {
                    Message message = packet.payload.message;

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
                                    matrix.put(vk, Evidence.Placeholder(from, Reason.Liar, true));
                                    break;
                                }

                                // Is the evidence included sufficient?
                                // TODO check whether this is a conflicting transaction.
                                matrix.put(from, Evidence.InsufficientFunds(blame.accused, blame.t));
                                break;
                            }
                            case EquivocationFailure: {
                                // These are the keys received by this player in the announcement phase.
                                Map<VerificationKey, EncryptionKey> receivedKeys = new HashMap<>();
                                fillBlameMatrixCollectHistory(vk, from, blame.packets, matrix,
                                        outputVectors, shuffleMessages, receivedKeys, sentKeys);

                                Queue<Address> addresses = new LinkedList<>();

                                // The last player will not have received a separate set of addresses for
                                // us to check, so we insert our own.
                                if (from.equals(players.get(N))) {
                                    addresses = newAddresses;
                                } else {

                                    Message output = outputVectors.get(from).payload.message;
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

                                System.out.println("Player " + me + ":" + vk + " checks eq. failure  from " + from + "; " + equivocationCheckHash(players, receivedKeys, addresses) + " against " + hashes.get(from));

                                // Check if this player correctly reported the hash previously sent to us.
                                if (!hashes.get(from).equals(equivocationCheckHash(players, receivedKeys, addresses))) {
                                    matrix.put(vk, Evidence.Placeholder(from, Reason.Liar, true));
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
                                if (packet.payload.signer != players.get(1)) {
                                    if (blame.privateKey == null)  {
                                        // TODO blame someone here.
                                    } else if (!blame.privateKey.EncryptionKey().equals(encryptionKeys.get(from))) {
                                        // TODO blame someone here.
                                    }
                                }

                                fillBlameMatrixCollectHistory(vk, from, blame.packets, matrix, outputVectors, shuffleMessages, new HashMap<VerificationKey, EncryptionKey>(), sentKeys);

                                break;
                            }
                            case DoubleSpend: {
                                // TODO does this actually spend the funds?
                                matrix.put(from, Evidence.DoubleSpend(blame.accused, blame.t));
                                break;
                            }
                            case InvalidSignature: {
                                if (blame.invalid == null || blame.accused == null) {
                                    matrix.put(vk, Evidence.Placeholder(from, Reason.Liar, true));
                                    break;
                                }

                                // TODO do we agree that the signature is invalid?
                                matrix.put(from, Evidence.InvalidSignature(blame.accused, blame.invalid));

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

                            matrix.put(vk, Evidence.Placeholder(to, Reason.Liar, true));
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
                        matrix.put(vk, Evidence.Placeholder(key, Reason.Liar, true));
                    }
                }

                List<Message> outputMessages = new LinkedList<>();
                for (SignedPacket packet : outputVectors.values()) {
                    outputMessages.add(packet.payload.message);
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
                    Evidence shuffleEvidence = checkShuffleMisbehavior(players, decryptionKeys, shuffleMessages,
                            outputEquivocate ? null : outputVectors);
                    if (shuffleEvidence != null) {
                        matrix.put(vk, shuffleEvidence);
                    }
                }
            }

            return matrix;
        }

        // Get the set of players from i to N.
        final public Set<VerificationKey> playerSet(int i, int n) throws CryptographyError, InvalidImplementationError {
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
        final public Set<VerificationKey> playerSet() throws CryptographyError, InvalidImplementationError {
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

            return crypto.hash(check);
        }

        // A round is a single run of the protocol.
        Round(  Machine machine,
                Map<Integer, VerificationKey> players,
                Address change,
                Mailbox mailbox) throws InvalidParticipantSetException {
            this.machine = machine;
            this.session = machine.session;
            this.amount = machine.amount;
            this.sk = machine.sk;
            this.players = players;
            this.change = change;
            vk = sk.VerificationKey();
            this.mailbox = mailbox;

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
                throw new InvalidParticipantSetException();
            }
        }
    }

    // Algorithm to randomly shuffle the elements of a message.
    final Message shuffle(Message message) throws CryptographyError, InvalidImplementationError, FormatException {
        Message shuffled = messages.make();

        // Read all elements of the packet and insert them in a Queue.
        Queue<Address> old = new LinkedList<>();
        int N = 0;
        while(!message.isEmpty()) {
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
    static void readAnnouncements(Map<VerificationKey, Message> messages,
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

    // This function is only called by fillBlameMatrix to collect messages sent in phases 1, 2, and 3.
    // and to organize the information appropriately.
    static void fillBlameMatrixCollectHistory(
            VerificationKey vk,
            VerificationKey from,
            Queue<SignedPacket> packets,
            Matrix matrix,
            // The messages sent in the broadcast phase by the last player to all the other players.
            Map<VerificationKey, SignedPacket> outputVectors,
             // The list of messages history in phase 2.
            Map<VerificationKey, SignedPacket> shuffleMessages,
            // The keys received by everyone in the announcement phase.
            Map<VerificationKey, EncryptionKey> receivedKeys,
            // The keys sent by everyone in the announcement phase.
            Map<VerificationKey, Map<VerificationKey, EncryptionKey>> sentKeys
    ) throws FormatException {

        if(packets == null) {
            log.error("Player " + vk.toString() + " null blames " + from.toString() + ", case J");
            matrix.put(vk, Evidence.Placeholder(from, Reason.Liar, true));
            return;
        }

        // Collect all packets received in the appropriate place.
        for (SignedPacket received : packets) {
            Packet packet = received.payload;
            switch (packet.phase) {
                case BroadcastOutput:
                    if (outputVectors.containsKey(from)) {
                        // We should only ever receive one such message from each player.
                        if (outputVectors.containsKey(from) && !outputVectors.get(from).equals(received)) {
                            log.error("Player " + vk.toString() + " null blames " + from.toString() + ", case A; " + outputVectors.get(from).toString() + " != " + received.payload.toString());
                            matrix.put(vk, Evidence.Placeholder(from, Reason.Liar, true));
                        }
                    }
                    outputVectors.put(from, received);
                    break;
                case Announcement:
                    Map<VerificationKey, EncryptionKey> map = sentKeys.get(packet.signer);
                    if (map == null) {
                        map = new HashMap<VerificationKey, EncryptionKey>();
                        sentKeys.put(packet.signer, map);
                    }

                    EncryptionKey key = packet.message.readEncryptionKey();
                    map.put(from, key);
                    receivedKeys.put(packet.signer, key);
                    break;
                case Shuffling:
                    if (shuffleMessages.containsKey(from)) {
                        // TODO blame someone here.
                    }
                    shuffleMessages.put(from, received);
                    break;
                default:
                    // TODO this case should never happen. It's not malicious but it's not allowed either.
                    log.error("Player " + vk.toString() + " null blames " + from.toString() + ", case C");
                    matrix.put(vk, Evidence.Placeholder(from, Reason.Liar, true));
                    break;
            }
        }
    }

    public static Evidence checkShuffleMisbehavior(
            Map<Integer, VerificationKey> players,
            Map<VerificationKey, DecryptionKey> decryptionKeys,
            Map<VerificationKey, SignedPacket> shuffleMessages,
            Map<VerificationKey, SignedPacket> broadcastMessages) throws FormatException {
        if (players == null || decryptionKeys == null ||
                shuffleMessages == null || broadcastMessages == null)
            throw new NullPointerException();

        SortedSet<Address> outputs = new TreeSet<>();

        // Go through the steps of shuffling messages.
        for (int i = 1; i <= players.size(); i++) {

            // The last step is from phase three, so we have to check for that.
            SignedPacket packet = null;
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

            Message message = packet.payload.message;

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

    protected Machine run(Machine state, Address change, Network network)  {

        // Get the initial ordering of the players.
        int i = 1;
        Map<Integer, VerificationKey> numberedPlayers = new TreeMap<>();
        for (VerificationKey player : state.players) {
            numberedPlayers.put(i, player);
            i++;
        }

        // Make an inbox for the next round.
        Mailbox mailbox = new Mailbox(state.session, state.sk, numberedPlayers.values(), network);

        try {
            this.new Round(state, numberedPlayers, change, mailbox).protocolDefinition();
        } catch (InterruptedException
                | ProtocolException
                | FormatException
                | ValueException
                | InvalidParticipantSetException
                | SignatureException
                | CoinNetworkException e) {
            state.e = e;
        }

        return state;
    }

    // Run the protocol without creating a new thread.
    public final Machine runProtocol(
            SessionIdentifier session, // Unique session identifier.
            long amount, // The amount to be shuffled per player.
            SigningKey sk, // The signing key of the current player.
            SortedSet<VerificationKey> players, // The set of players, sorted alphabetically by address.
            Address change, // Change address. (can be null)
            Network network, // The network that connects us to the other players.
            // If this is not null, the machine is put in this channel so that another thread can
            // query the phase as it runs.
            SendChan<Machine> queue
    ) {
        if (amount <= 0) {
            throw new IllegalArgumentException();
        }
        if (session == null || sk == null || players == null || network == null) {
            throw new NullPointerException();
        }
        Machine machine = new Machine(session, amount, sk, players);
        if (queue != null) {
            queue.send(machine);
        }

        return run(machine, change, network);
    }

    // Run the protocol in a separate thread and get a future to the final state.
    public final Future<Machine> runProtocolFuture(
            final SessionIdentifier session, // Unique session identifier.
            final long amount, // The amount to be shuffled per player.
            final SigningKey sk, // The signing key of the current player.
            final SortedSet<VerificationKey> players, // The set of players, sorted alphabetically by address.
            final Address change, // Change address. (can be null)
            final Network network // The network that connects us to the other players.
    ) {
        final Chan<Machine> q = new Chan<>();

        if (amount <= 0) {
            throw new IllegalArgumentException();
        }
        if (session == null || sk == null || players == null || network == null) {
            throw new NullPointerException();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean sent = q.send(runProtocol(session, amount, sk, players, change, network, null));
                } catch (TimeoutError e) {
                    log.error("Protocol timeout error.");
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("Exception caught: " + e.toString());
                }

                q.close();
            }
        }).start();

        return new Future<Machine>() {
            Machine result = null;
            boolean done = false;

            @Override
            public boolean cancel(boolean b) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return done || q.closed();
            }

            @Override
            public synchronized Machine get() throws InterruptedException, ExecutionException {
                if (done) {
                    return result;
                }

                result = q.receive();
                done = true;
                return result;
            }

            @Override
            public synchronized Machine get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
                if (done) {
                    return result;
                }

                Machine r = q.receive(l, timeUnit);

                if (r != null) {
                    result = r;
                    done = true;
                    return result;
                }

                if (q.closed()) {
                    done = true;
                }

                return null;
            }
        };
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
}
