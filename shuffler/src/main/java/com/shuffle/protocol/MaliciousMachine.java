package com.shuffle.protocol;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Coin;
import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.blame.Blame;
import com.shuffle.protocol.blame.BlameException;
import com.shuffle.protocol.blame.Matrix;

import java.net.ProtocolException;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;

/**
 * An extension of CoinShuffle that includes a lot of malicious behavior for testing purposes.
 *
 * Created by Daniel Krawisz on 2/3/16.
 */
final public class MaliciousMachine extends CoinShuffle {

    // This malicious adversary sends different encryption keys to different adversariaes.
    public class AnnouncementEquivocator extends ShuffleMachine {
        Set<VerificationKey> equivocate;

        AnnouncementEquivocator(
                SessionIdentifier session,
                long amount, SigningKey sk,
                SortedSet<VerificationKey> players,
                Set<VerificationKey> equivocate) {
            super(session, amount, sk, players);
            this.equivocate = equivocate;
        }

        class MaliciousRound extends Round {
            DecryptionKey alt = null;

            MaliciousRound(Map<Integer, VerificationKey> players, Address change, Mailbox mailbox) throws InvalidParticipantSetException {
                super(players, change, mailbox);
            }

            @Override
            DecryptionKey newDecryptionKey(Map<VerificationKey, Address> changeAddresses) {
                DecryptionKey dk = null;
                if (me != 1) {
                    dk = crypto.makeDecryptionKey();
                    alt = crypto.makeDecryptionKey();

                    // Broadcast the public key and store it in the set with everyone else's.
                    encryptionKeys.put(vk, dk.EncryptionKey());
                    changeAddresses.put(vk, change);
                    Message message = messages.make().attach(dk.EncryptionKey());
                    Message eq = messages.make().attach(alt.EncryptionKey());

                    if (change != null) {
                        message.attach(change);
                    }

                    for (VerificationKey to : players.values()) {
                        mailbox.send(new Packet((equivocate.contains(to) ? eq : message), session, phase, vk, to));
                    }
                }
                return dk;
            }

            @Override
            Matrix equivocationCheck(
                    Map<VerificationKey, EncryptionKey> encryptonKeys,
                    Queue<Address> newAddresses)
                    throws InterruptedException, ValueException,
                    FormatException, ProtocolException,
                    BlameException, SignatureException {

                Map<VerificationKey, EncryptionKey> otherKeys = new HashMap<>();
                otherKeys.putAll(encryptonKeys);

                if (alt != null) {
                    otherKeys.put(vk, alt.EncryptionKey());
                }

                Message equivocationCheck = equivocationCheckHash(players, encryptonKeys, newAddresses);
                Message otherCheck = equivocationCheckHash(players, otherKeys, newAddresses);

                if (!equivocationCheckSent) {
                    for (VerificationKey to : players.values()) {
                        mailbox.send(new Packet((equivocate.contains(to) ? otherCheck : equivocationCheck), session, phase, vk, to));
                    }

                    equivocationCheckSent = true;
                }

                // Wait for a similar message from everyone else and check that the result is the name.
                Map<VerificationKey, Message> hashes = mailbox.receiveFromMultiple(playerSet(1, players.size()), phase, false);
                hashes.put(vk, equivocationCheck);

                if (areEqual(hashes.values())) {
                    if (mailbox.blameReceived()) {
                        fillBlameMatrix(new Matrix());
                    }

                    return null;
                }

                // If the hashes are not equal, enter the blame phase.
                // Collect all packets from phase 1 and 3.
                phase = Phase.Blame;
                Queue<SignedPacket> evidence = mailbox.getPacketsByPhase(Phase.Announcement);
                evidence.addAll(mailbox.getPacketsByPhase(Phase.BroadcastOutput));
                Message blameMessage = messages.make().attach(Blame.EquivocationFailure(evidence));
                mailbox.broadcast(blameMessage, phase);

                return fillBlameMatrix(new Matrix());
            }
        }

        @Override
        ReturnState run(Address change, Network network)  {
            // Get the initial ordering of the players.
            int i = 1;
            Map<Integer, VerificationKey> numberedPlayers = new TreeMap<>();
            for (VerificationKey player : players) {
                numberedPlayers.put(i, player);
                i++;
            }

            // Make an inbox for the next round.
            Mailbox mailbox = new Mailbox(session, sk, numberedPlayers.values(), network);

            try {
                new MaliciousRound(numberedPlayers, change, mailbox).protocolDefinition();
            } catch (InterruptedException
                    | ProtocolException
                    | FormatException
                    | ValueException
                    | InvalidParticipantSetException
                    | SignatureException e) {
                return new ReturnState(false, session, phase, e, null);
            }

            if (matrix == null) {
                return new ReturnState(true, session, phase, null, null);
            }

            return new ReturnState(false, session, phase, null, matrix);
        }
    }

    // Send different output vectors to different players during the broadcast phase.
    public class BroadcastEquivocator extends ShuffleMachine {
        Set<VerificationKey> equivocate;

        BroadcastEquivocator(
                SessionIdentifier session,
                long amount, SigningKey sk,
                SortedSet<VerificationKey> players,
                Set<VerificationKey> equivocate) {
            super(session, amount, sk, players);
            this.equivocate = equivocate;
        }

        class MaliciousRound extends Round {
            Deque<Address> otherAddresses;

            MaliciousRound(Map<Integer, VerificationKey> players, Address change, Mailbox mailbox) throws InvalidParticipantSetException {
                super(players, change, mailbox);
            }

            @Override
            Deque<Address> readAndBroadcastNewAddresses(Message shuffled)
                    throws FormatException, InterruptedException,
                    SignatureException, ValueException, BlameException {
                Deque<Address> newAddresses;
                if (me == N) {
                    newAddresses = readNewAddresses(shuffled);
                    Message otherShuffled = shuffle(shuffled);
                    otherAddresses = readNewAddresses(otherShuffled);

                    for (VerificationKey to : players.values()) {
                        mailbox.send(new Packet((equivocate.contains(to) ? otherShuffled : shuffled), session, phase, vk, to));
                    }
                } else {
                    newAddresses = readNewAddresses(mailbox.receiveFrom(players.get(N), phase));
                }

                return newAddresses;
            }

            @Override
            Matrix equivocationCheck(
                    Map<VerificationKey, EncryptionKey> encryptonKeys,
                    Queue<Address> newAddresses)
                    throws InterruptedException, ValueException,
                    FormatException, ProtocolException,
                    BlameException, SignatureException {

                Message equivocationCheck = equivocationCheckHash(players, encryptonKeys, newAddresses);
                Message otherCheck = equivocationCheckHash(players, encryptonKeys, otherAddresses);

                if (!equivocationCheckSent) {
                    for (VerificationKey to : players.values()) {
                        mailbox.send(new Packet((equivocate.contains(to) ? otherCheck : equivocationCheck), session, phase, vk, to));
                    }

                    equivocationCheckSent = true;
                }

                // Wait for a similar message from everyone else and check that the result is the name.
                Map<VerificationKey, Message> hashes = mailbox.receiveFromMultiple(playerSet(1, players.size()), phase, false);
                hashes.put(vk, equivocationCheck);

                if (areEqual(hashes.values())) {
                    if (mailbox.blameReceived()) {
                        fillBlameMatrix(new Matrix());
                    }

                    return null;
                }

                // If the hashes are not equal, enter the blame phase.
                // Collect all packets from phase 1 and 3.
                phase = Phase.Blame;
                Queue<SignedPacket> evidence = mailbox.getPacketsByPhase(Phase.Announcement);
                evidence.addAll(mailbox.getPacketsByPhase(Phase.BroadcastOutput));
                Message blameMessage = messages.make().attach(Blame.EquivocationFailure(evidence));
                mailbox.broadcast(blameMessage, phase);

                return fillBlameMatrix(new Matrix());
            }
        }

        @Override
        ReturnState run(Address change, Network network)  {
            // Get the initial ordering of the players.
            int i = 1;
            Map<Integer, VerificationKey> numberedPlayers = new TreeMap<>();
            for (VerificationKey player : players) {
                numberedPlayers.put(i, player);
                i++;
            }

            // Make an inbox for the next round.
            Mailbox mailbox = new Mailbox(session, sk, numberedPlayers.values(), network);

            try {
                new MaliciousRound(numberedPlayers, change, mailbox).protocolDefinition();
            } catch (InterruptedException
                    | ProtocolException
                    | FormatException
                    | ValueException
                    | InvalidParticipantSetException
                    | SignatureException e) {
                return new ReturnState(false, session, phase, e, null);
            }

            if (matrix == null) {
                return new ReturnState(true, session, phase, null, null);
            }

            return new ReturnState(false, session, phase, null, matrix);
        }
    }

    // Drop an address during the shuffle phase.
    public class DropAddress extends ShuffleMachine {
        int drop;

        DropAddress(SessionIdentifier session, long amount, SigningKey sk, SortedSet<VerificationKey> players, int drop) {
            super(session, amount, sk, players);
            this.drop = drop;
        }

        class MaliciousRound extends Round {

            MaliciousRound(Map<Integer, VerificationKey> players, Address change, Mailbox mailbox) throws InvalidParticipantSetException {
                super(players, change, mailbox);
            }
        }
    }

    // Drop address and replace it with a duplicate during the Shuffle phase.
    public class DropAddressReplaceDuplicate extends ShuffleMachine {
        int drop;
        int replace;

        DropAddressReplaceDuplicate(
                SessionIdentifier session,
                long amount, SigningKey sk,
                SortedSet<VerificationKey> players,
                int drop, int replace) {
            super(session, amount, sk, players);
            this.drop = drop;
            this.replace = replace;
        }

        class MaliciousRound extends Round {

            MaliciousRound(Map<Integer, VerificationKey> players, Address change, Mailbox mailbox) throws InvalidParticipantSetException {
                super(players, change, mailbox);
            }
        }
    }

    // Drop an address and replace it with a new one during the shuffle phase.
    public class DropAddressReplaceNew extends ShuffleMachine {
        int drop;
        Address replace;

        DropAddressReplaceNew(
                SessionIdentifier session,
                long amount, SigningKey sk,
                SortedSet<VerificationKey> players,
                int drop, Address replace) {
            super(session, amount, sk, players);
            this.drop = drop;
            this.replace = replace;
        }

        class MaliciousRound extends Round {

            MaliciousRound(Map<Integer, VerificationKey> players, Address change, Mailbox mailbox) throws InvalidParticipantSetException {
                super(players, change, mailbox);
            }
        }
    }

    public MaliciousMachine(MessageFactory messages, Crypto crypto, Coin coin) {
        super(messages, crypto, coin);
    }
}
