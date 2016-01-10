package com.shuffle.protocol;

import com.shuffle.cryptocoin.Address;
import com.shuffle.cryptocoin.Coin;
import com.shuffle.cryptocoin.CoinNetworkError;
import com.shuffle.cryptocoin.Crypto;
import com.shuffle.cryptocoin.CryptographyError;
import com.shuffle.cryptocoin.DecryptionKey;
import com.shuffle.cryptocoin.EncryptionKey;
import com.shuffle.cryptocoin.SigningKey;
import com.shuffle.cryptocoin.Transaction;
import com.shuffle.cryptocoin.VerificationKey;

import java.net.ProtocolException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;

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
final class CoinShuffle {

    final Crypto crypto;

    final Coin coin;

    final MessageFactory messages;

    final Network network;

    public class ShuffleMachine {
        Phase phase;

        final SessionIdentifier τ;

        final long amount; // The amount to be shuffled.

        final private SigningKey sk; // My signing private key.

        final private VerificationKey vk; // My verification public key, which is also my identity.

        final SortedSet<VerificationKey> players;

        final Address change;

        final int maxRetries;

        final int minPlayers;

        // the phase can be accessed concurrently in case we want to update
        // the user on how the protocol is going.
        public Phase currentPhase() {
            return phase;
        }

        // A single round of the protocol. It is possible that the players may go through
        // several failed rounds until they have eliminated malicious players.
        class Round {

            final private int me; // Which player am I?

            final private Map<Integer, VerificationKey> players; // The keys representing all the players, in order.

            final private int N; // The number of players.

            // This will contain the new encryption public keys.
            final Map<VerificationKey, EncryptionKey> encryptionKeys = new HashMap<>();

            final Address change; // My change address. (may be null).

            BlameMatrix protocolDefinition(
            ) throws
                    TimeoutError,
                    FormatException,
                    CryptographyError,
                    InvalidImplementationError,
                    ValueException,
                    CoinNetworkError,
                    InterruptedException, ProtocolException {

                if (amount <= 0) {
                    throw new IllegalArgumentException();
                }

                // Phase 1: Announcement
                // In the announcement phase, participants distribute temporary encryption keys.
                phase = Phase.Announcement;

                // Check for sufficient funds.
                // There was a problem with the wording of the original paper which would have meant
                // that player 1's funds never would have been checked, but we have to do that.
                BlameMatrix matrix = blameInsufficientFunds();
                if (matrix != null) {
                    return matrix;
                }

                // This will contain the change addresses.
                Map<VerificationKey, Address> change = new HashMap<>();

                // Everyone except player 1 creates a new keypair and sends it around to everyone else.
                DecryptionKey dk = null;
                EncryptionKey ek;
                if (me != 1) {
                    dk = crypto.DecryptionKey();
                    ek = dk.EncryptionKey();

                    // Broadcast the privateKey and store it in the set with everyone else's.
                    encryptionKeys.put(vk, ek);
                    change.put(vk, this.change);
                    Message message = messages.make().attach(ek);
                    if (this.change != null) {
                        message.attach(this.change);
                    }
                    broadcast(message);
                }

                // Now we wait to receive similar inbox from everyone else.
                try {
                    Map<VerificationKey, Message> σ1 = receiveFromMultiple(playerSet(2, N), phase);
                    readAnouncements(σ1, change);
                } catch (BlameException e) {
                    // TODO : might receive blame messages about insufficient funds.
                }

                // Phase 2: Shuffle
                // In the shuffle phase, we create a sequence of orderings which will b successively
                // applied by each particpant. Everyone has the incentive to insert their own address
                // at a random location, which is sufficient to ensure randomness of the whole thing
                // to all participants.
                phase = Phase.Shuffling;

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
                        σ2 = decryptAll(σ2.attach(receiveFrom(players.get(me - 1), phase)), dk, me - 1);
                    }

                    // Add our own address to the mix. Note that if me == N, ie, the last player, then no
                    // encryption is done. That is because we have reached the last layer of encryption.
                    Address encrypted = addr_new;
                    for (int i = N; i > me; i--) {
                        // Successively encrypt with the keys of the players who haven't had their turn yet.
                        encrypted = encryptionKeys.get(players.get(i)).encrypt(encrypted);
                    }

                    // Insert new entry and reorder the keys.
                    σ2 = shuffle(σ2.attach(encrypted));

                    // Pass it along to the next player.
                    if (me != N) {
                        send(new Packet(σ2, τ, phase, vk, players.get(me + 1)));
                    }

                    // Phase 3: broadcast outputs.
                    // In this phase, the last player just broadcasts the transaction to everyone else.
                    phase = Phase.BroadcastOutput;

                    if (me == N) {
                        // The last player adds his own new address in without encrypting anything and shuffles the result.
                        newAddresses = readNewAddresses(σ2);
                        broadcast(σ2);
                    } else {
                        newAddresses = readNewAddresses(receiveFrom(players.get(N), phase));
                    }

                    // Everyone else receives the broadcast and checks to make sure their message was included.
                    if (!newAddresses.contains(addr_new)) {
                        // TODO handle this case.
                    }

                    // Phase 4: equivocation check.
                    // In this phase, participants check whether any player has history different
                    // encryption keys to different players.
                    phase = Phase.EquivocationCheck;

                    matrix = equivocationCheck(encryptionKeys, vk);
                    if (matrix != null) {
                        return matrix;
                    }
                } catch (BlameException e) {
                    // TODO might receive messages about failed shuffles or failed equivocation check.
                }

                // Phase 5: verification and submission.
                // Everyone creates a Bitcoin transaction and signs it, then broadcasts the signature.
                // If all signatures check out, then the transaction is history into the net.
                phase = Phase.VerificationAndSubmission;

                List<VerificationKey> inputs = new LinkedList<>();
                for (int i = 1; i <= N; i++) {
                    inputs.add(players.get(i));
                }
                Transaction t = coin.shuffleTransaction(amount, inputs, newAddresses, change);
                broadcast(messages.make().attach(sk.makeSignature(t)));

                try {
                    Map<VerificationKey, Message> σ5 = receiveFromMultiple(playerSet(1, N), phase);

                    // Verify the signatures.
                    for (Map.Entry<VerificationKey, Message> sig : σ5.entrySet()) {
                        if (!sig.getKey().verify(t, sig.getValue().readSignature())) {
                            // TODO handle this case.
                        }
                    }
                } catch (BlameException e) {
                    // TODO might get something about invalid signatures.
                }

                // Send the transaction into the net.
                // TODO blame someone if a double spend is detected.
                System.out.println("Player " + me + " is about to send transaction " + t);
                coin.send(t);

                // The protocol has completed successfully.
                phase = Phase.Completed;

                return null;
            }

            void readAnouncements(Map<VerificationKey, Message> messages,
                                  Map<VerificationKey, Address> change) throws FormatException {
                for (Map.Entry<VerificationKey, Message> entry : messages.entrySet()) {
                    VerificationKey key = entry.getKey();
                    Message message = entry.getValue();

                    encryptionKeys.put(key, message.readEncryptionKey());

                    if (!message.isEmpty()) {
                        change.put(key, message.readAddress());
                    }
                }
            }

            Queue<Address> readNewAddresses(Message message) throws FormatException, InvalidImplementationError {
                Queue<Address> queue = new LinkedList<>();

                Message copy = messages.copy(message);
                while (!copy.isEmpty()) {
                    queue.add(copy.readAddress());
                }

                return queue;
            }

            Message decryptAll(Message message, DecryptionKey key, int expected) throws InvalidImplementationError, FormatException {
                Message decrypted = messages.make();

                int count = 1;
                Set<Address> addrs = new HashSet<>(); // Used to check that all addresses are different.

                Message copy = messages.copy(message);
                while (!copy.isEmpty()) {
                    Address address = copy.readAddress();
                    addrs.add(address);
                    count++;
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
            private BlameMatrix equivocationCheck(
                    Map<VerificationKey, EncryptionKey> encryptonKeys,
                    VerificationKey vk) throws InterruptedException, ValueException, FormatException, ProtocolException, BlameException {
                // Put all temporary encryption keys into a list and hash the result.
                Message σ4 = messages.make();
                for (int i = 2; i <= players.size(); i++) {
                    σ4.attach(encryptonKeys.get(players.get(i)));
                }

                σ4 = crypto.hash(σ4);
                broadcast(σ4);

                // Wait for a similar message from everyone else and check that the result is the name.
                Map<VerificationKey, Message> hashes = receiveFromMultiple(playerSet(1, players.size()), phase);
                hashes.put(vk, σ4);
                if (areEqual(hashes.values())) {
                    return null;
                }

                // If the hashes are not equal, enter the blame phase.
                // Collect all packets from phase 1 and 3.
                phase = Phase.Blame;
                Message blameMessage = messages.make();
                List<Packet> evidence = getPacketsByPhase(Phase.Announcement);
                evidence.addAll(getPacketsByPhase(Phase.BroadcastOutput));
                blameMessage.attach(new BlameMatrix.Blame(evidence));
                broadcast(blameMessage);

                return fillBlameMatrix(new BlameMatrix());
            }

            // Check for players with insufficient funds. This happens in phase 1 and phase 5.
            private BlameMatrix blameInsufficientFunds() throws InterruptedException, FormatException, ValueException {
                List<VerificationKey> offenders = new LinkedList<>();

                System.out.println("player " + me + " about to try insufficient funds thinky. ");

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
                System.out.println("player " + me + " finds offenders " + offenders.toString());

                // If not, enter blame phase and find offending transactions.
                phase = Phase.Blame;
                BlameMatrix matrix = new BlameMatrix();
                Message blameMessage = messages.make();
                for (VerificationKey offender : offenders) {
                    Transaction t = coin.getOffendingTransaction(offender.address(), amount);

                    if (t == null) {
                        blameMessage.attach(new BlameMatrix.Blame(offender));
                        matrix.add(vk, offender,
                                new BlameMatrix.BlameEvidence(BlameMatrix.BlameReason.NoFundsAtAll, true));
                    } else {
                        blameMessage.attach(new BlameMatrix.Blame(offender, t, BlameMatrix.BlameReason.InsufficientFunds));
                        matrix.add(vk, offender,
                                new BlameMatrix.BlameEvidence(BlameMatrix.BlameReason.InsufficientFunds, true, t));
                    }
                }

                // Broadcast offending transactions.
                broadcast(blameMessage);

                // Get all subsequent blame messages.
                return fillBlameMatrix(matrix);
            }

            // Some misbehavior that has occurred during the shuffle phase.
            private BlameMatrix blameShuffleMisbehavior(DecryptionKey dk) throws InterruptedException, FormatException, ValueException, ProtocolException, BlameException {
                // First skip to phase 4 and do an equivocation check.
                phase = Phase.EquivocationCheck;
                BlameMatrix matrix = equivocationCheck(encryptionKeys, vk);

                // If we get a blame matrix back, that means that the culprit was found.
                if (matrix != null) {
                    return matrix;
                }

                // Otherwise, there are some more things we have to check.
                phase = Phase.Blame;

                // Collect all packets from phase 2 and 3.
                Message blameMessage = messages.make();
                List<Packet> evidence = getPacketsByPhase(Phase.Shuffling);
                evidence.addAll(getPacketsByPhase(Phase.BroadcastOutput));

                // Send them all with the decryption key.
                blameMessage.attach(new BlameMatrix.Blame(dk, evidence));
                broadcast(blameMessage);

                return fillBlameMatrix(new BlameMatrix());
            }

            // When we know we'll receive a bunch of blame messages, we have to go through them all to figure
            // out what's going on.
            private BlameMatrix fillBlameMatrix(BlameMatrix matrix) throws InterruptedException, FormatException, ValueException {
                Map<VerificationKey, List<Packet>> blameMessages = receiveAllBlame();

                // Get all hashes received in phase 4 so that we can check that they were reported correctly.
                List<Packet> hashMessages = getPacketsByPhase(Phase.EquivocationCheck);
                Map<VerificationKey, Message> hashes = new HashMap<>();
                for (Packet packet : hashMessages) {
                    hashes.put(packet.signer, packet.message);
                }

                // The messages history in the broadcast phase by the last player to all the other players.
                Map<VerificationKey, Message> outputVectors = new HashMap<>();

                // The encryption keys history from every player to every other.
                Map<VerificationKey, Map<VerificationKey, EncryptionKey>> sentKeys = new HashMap<>();

                // The list of messages history in phase 2.
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
                                    credible = coin.isOffendingTransaction(blame.accused.address(), amount, blame.t);
                                    matrix.add(blame.accused, from,
                                            new BlameMatrix.BlameEvidence(BlameMatrix.BlameReason.NoFundsAtAll, credible));
                                    break;
                                case EquivocationFailure:
                                    Map<VerificationKey, EncryptionKey> receivedKeys = new HashMap<>();

                                    // Collect all packets received in the appropriate place.
                                    for (Packet received : blame.packets) {
                                        switch (received.phase) {
                                            case BroadcastOutput:
                                                if (outputVectors.containsKey(from)) {
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
                                    if (decryptionKeys.containsKey(from)) {
                                        // TODO blame someone here.
                                    }
                                    decryptionKeys.put(from, blame.privateKey);

                                    // Check that the decryption key is valid.
                                    if (!blame.privateKey.EncryptionKey().equals(encryptionKeys.get(from))) {
                                        // TODO blame someone here.
                                    }

                                    // Collect all packets received in the appropriate place.
                                    for (Packet received : blame.packets) {
                                        switch (received.phase) {
                                            case BroadcastOutput:
                                                if (outputVectors.containsKey(packet.signer)) {
                                                    // TODO blame someone here.
                                                }
                                                outputVectors.put(packet.signer, received.message);
                                                break;
                                            case Shuffling:
                                                if (shuffleMessages.containsKey(packet.signer)) {
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
                    Set<VerificationKey> leftover = playerSet(1, players.size() - 1);
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
                                // TODO blame player to. He should have history us this.
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
                    Set<VerificationKey> leftover = playerSet(1, players.size() - 1);
                    leftover.removeAll(outputVectors.keySet());
                    if (leftover.size() > 0) {
                        // TODO blame someone.
                    } else {
                        SortedSet<Address> outputs = new TreeSet<>();

                        for (int i = 2; i < players.size(); i++) {
                            Message message = shuffleMessages.get(players.get(i));

                            // Grab the correct number of addresses and decrypt them.
                            SortedSet<Address> addresses = new TreeSet<>();
                            for (int j = 0; j < i - 2; j++) {
                                Address address = message.readAddress();
                                if (address == null) {
                                    // TODO blame someone.
                                }
                                for (int k = players.size(); k >= 2; k--) {
                                    address = decryptionKeys.get(players.get(i)).decrypt(address);
                                }
                                addresses.add(address);
                            }

                            if (addresses.size() != i - 1) {
                                // TODO blame someone.
                            }

                            addresses.removeAll(outputs);

                            if (addresses.size() != 1) {
                                // TODO blame someone.
                            }

                            outputs.add(addresses.first());
                        }

                        // We should not have been able to get this far.
                        // TODO Blame someone!
                    }
                }

                return matrix;
            }

            final Queue<Packet> delivered = new LinkedList<>(); // A queue of messages that has been delivered that we aren't ready to look at yet.
            final Queue<Packet> history = new LinkedList<>(); // All messages that have been history.

            // Get the set of players from i to N.
            public Set<VerificationKey> playerSet(int i, int n) throws CryptographyError, InvalidImplementationError {
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

            public Set<VerificationKey> playerSet() throws CryptographyError, InvalidImplementationError {
                return playerSet(1, N);
            }

            public void broadcast(Message message) throws TimeoutError, CryptographyError, InvalidImplementationError {
                Set<VerificationKey> keys = playerSet();

                for (VerificationKey to : keys) {
                    if (!to.equals(sk.VerificationKey())) {
                        send(new Packet(message, τ, phase, vk, to));
                    }
                }
            }

            public void send(Packet packet) throws TimeoutError, CryptographyError, InvalidImplementationError {
                network.sendTo(packet.recipient, packet);
                history.add(packet);
            }

            // This method should only be called by receiveNextPacket
            private Packet findPacket(Phase expectedPhase) throws InterruptedException, ValueException {
                // Go through the queue of received messages if any are there.
                if (delivered.size() > 0) {
                    Iterator<Packet> i = delivered.iterator();
                    while (i.hasNext()) {
                        Packet packet = i.next();

                        // Return any that matches what we're looking for.
                        if (expectedPhase == packet.phase) {
                            i.remove();
                            return packet;
                        }
                    }
                }

                // Now we wait for the right message from the network, since we haven't already received it.
                while (true) {
                    Packet packet = network.receive();
                    Phase phase = packet.phase;

                    // Check that this is someone in the same session of this protocol as us.
                    if (!τ.equals(packet.τ)) {
                        throw new ValueException(ValueException.Values.τ, τ.toString(), packet.τ.toString());
                    }

                    // Check that this message is intended for us.
                    if (!packet.recipient.equals(sk.VerificationKey())) {
                        throw new ValueException(ValueException.Values.recipient, sk.VerificationKey().toString(), packet.recipient.toString());
                    }

                    if (expectedPhase == phase || phase == Phase.Blame) {
                        return packet;
                    }

                    delivered.add(packet);
                }
            }

            // Get the next message from the phase we're in. It's possible for other players to get
            // ahead under some circumstances, so we have to keep their messages to look at later.
            Packet receiveNextPacket(Phase expectedPhase)
                    throws FormatException, CryptographyError,
                    InterruptedException, TimeoutError, InvalidImplementationError, ValueException, BlameException {

                Packet packet = findPacket(expectedPhase);
                history.add(packet);
                if (packet.phase == Phase.Blame) {
                    throw new BlameException(packet.signer, packet);
                }
                return packet;
            }

            // Get all packets history or received by phase. Used during blame phase.
            public List<Packet> getPacketsByPhase(Phase phase) {
                List<Packet> selection = new LinkedList<>();

                for (Packet packet : history) {
                    if (packet.phase == phase) {
                        selection.add(packet);
                    }
                }

                for (Packet packet : delivered) {
                    if (packet.phase == phase) {
                        selection.add(packet);
                    }
                }

                return selection;
            }

            public Message receiveFrom(VerificationKey from, Phase expectedPhase)
                    throws TimeoutError, CryptographyError, FormatException, ValueException,
                    InvalidImplementationError, InterruptedException, BlameException {

                Packet packet = receiveNextPacket(expectedPhase);

                // If we receive a message, but it is not from the expected source, it might be a blame message.
                if (!from.equals(packet.signer)) {
                    throw new ValueException(ValueException.Values.phase, packet.phase.toString(), expectedPhase.toString());
                }

                return packet.message;
            }

            public Map<VerificationKey, Message> receiveFromMultiple(Set<VerificationKey> from, Phase expectedPhase)
                    throws TimeoutError, CryptographyError, FormatException,
                    InvalidImplementationError, ValueException, InterruptedException, ProtocolException, BlameException {

                // Collect the messages in here.
                Map<VerificationKey, Message> broadcasts = new HashMap<>();

                // Don't receive a message from myself.
                from.remove(sk.VerificationKey());

                while (from.size() > 0) {
                    Packet packet = receiveNextPacket(expectedPhase);
                    VerificationKey sender = packet.signer;

                    if(broadcasts.containsKey(sender)) {
                        throw new ProtocolException();
                    }
                    broadcasts.put(sender, packet.message);
                    from.remove(sender);
                }

                return broadcasts;
            }

            // When the blame phase it reached, there may be a lot of blame going around. This function
            // waits to receive all blame messages until a timeout exception is caught, and then returns
            // the list of blame messages, organized by player.
            public Map<VerificationKey, List<Packet>> receiveAllBlame() throws InterruptedException, FormatException, ValueException {
                Map<VerificationKey, List<Packet>> blame = new HashMap<>();
                for (VerificationKey player : players.values()) {
                    blame.put(player, new LinkedList<Packet>());
                }

                while(true) {
                    try {
                        Packet next = receiveNextPacket(Phase.Blame);
                        blame.get(next.signer).add(next);
                    } catch (BlameException e) {
                        // This shouldn't really happen but just in case.
                        blame.get(e.packet.signer).add(e.packet);
                    } catch (TimeoutError e) {
                        break;
                    }
                }

                // Get the blame messages we history too. Just get everything!
                for (Packet packet : history) {
                    if (packet.phase == Phase.Blame) {
                        blame.get(sk.VerificationKey()).add(packet);
                    }
                }

                return blame;
            }

            Round(Map<Integer, VerificationKey> players, Address change) throws InvalidParticipantSetException {
                this.players = players;
                this.change = change;

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

        // Run the protocol. This function manages retries and error cases. The core loop is
        // in the function protocolDefinition above.
        ReturnState run() throws InvalidImplementationError, InterruptedException {

            // Don't let the protocol be run more than once at a time.
            if (phase != Phase.Uninitiated) {
                return new ReturnState(false, τ, currentPhase(), new ProtocolStartedException(), null);
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
                            i++;
                        }
                    }

                    // Send an introductory message and make sure all players agree on who is in
                    // this round of the protocol.
                    // TODO

                    // Run the protocol.
                    try {
                        blame = new Round(numberedPlayers, change).protocolDefinition();
                    } catch (TimeoutError e) {
                        // TODO We have to go into "suspect" mode at this point to determine why the timeout occurred.
                        return new ReturnState(false, τ, currentPhase(), e, null);
                    }

                    Phase endPhase = currentPhase();

                    if (endPhase != Phase.Blame) {
                        // The protocol was successful, so return.
                        return new ReturnState(true, τ, endPhase, null, null);
                    }

                    attempt++;

                    if (attempt > maxRetries) {
                        break;
                    }

                    // Eliminate malicious players if possible and try again.
                    phase = Phase.Uninitiated;

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

                return new ReturnState(false, τ, Phase.Blame, null, blame);
            } catch (InvalidParticipantSetException
                    | ProtocolException
                    | ValueException
                    | CoinNetworkError
                    | CryptographyError
                    | FormatException e) {
                // TODO many of these cases could be dealt with instead of just aborting.
                return new ReturnState(false, τ, currentPhase(), e, null);
            }
        }

        // The ShuffleMachine cannot be instantiated directly.
        ShuffleMachine(
                SessionIdentifier τ,
                long amount,
                SigningKey sk,
                SortedSet<VerificationKey> players,
                Address change,
                int maxRetries,
                int minPlayers) {

            if (τ == null || sk == null || players == null) {
                throw new NullPointerException();
            }

            if (amount <= 0) {
                throw new IllegalArgumentException();
            }

            this.τ = τ;
            this.amount = amount;
            this.sk = sk;
            this.vk = sk.VerificationKey();
            this.players = players;
            this.change = change;
            this.maxRetries = maxRetries;
            this.minPlayers = minPlayers;
            this.phase = Phase.Uninitiated;
        }
    }

    // Algorithm to randomly shuffle the elements of a message.
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

    // Test whether a set of messages are equal.
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

    // Run the protocol without creating a new thread.
    public ReturnState run(
            SessionIdentifier τ, // Unique session identifier.
            long amount, // The amount to be shuffled per player.
            SigningKey sk, // The signing key of the current player.
            SortedSet<VerificationKey> players, // The set of players, sorted alphabetically by address.
            Address change, // Change address. (can be null)
            int maxRetries, // maximum number of rounds this protocol can go through.,
            int minPlayers, // Minimum number of players allowed for the protocol to continue.
            // If this is not null, the machine is put in this queue so that another thread can
            // query the phase as it runs.
            LinkedBlockingQueue<ShuffleMachine> queue
    ) throws InvalidImplementationError, InterruptedException {
        if (amount <= 0) {
            throw new IllegalArgumentException();
        }
        ShuffleMachine machine = new ShuffleMachine(τ, amount, sk, players, change, maxRetries, minPlayers);
        if (queue != null) {
            queue.add(machine);
        }
        return machine.run();
    }

    public CoinShuffle(
            MessageFactory messages, // Object that knows how to create and copy messages.
            Crypto crypto, // Connects to the cryptography.
            Coin coin, // Connects us to the Bitcoin or other cryptocurrency netork.
            Network network // Connects us to the other shuffle players.
    ) {
        if (crypto == null || coin == null || messages == null || network == null) {
            throw new NullPointerException();
        }
        this.crypto = crypto;
        this.coin = coin;
        this.messages = messages;
        this.network = network;
    }
}
