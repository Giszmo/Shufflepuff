package com.shuffle.protocol;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Coin;
import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.blame.Blame;
import com.shuffle.protocol.blame.BlameException;
import com.shuffle.protocol.blame.Matrix;

import java.net.ProtocolException;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

/**
 * An extension of CoinShuffle that includes a lot of malicious behavior for testing purposes.
 *
 * Created by Daniel Krawisz on 2/3/16.
 */
final public class MaliciousMachine extends CoinShuffle {

    private final class AnnouncementEquivocatorRound extends Round {
        final Set<VerificationKey> equivocate;
        DecryptionKey alt = null;

        AnnouncementEquivocatorRound(
                Machine machine,
                Map<Integer, VerificationKey> players,
                Address change,
                Mailbox mailbox,
                Set<VerificationKey> equivocate) throws InvalidParticipantSetException {
            super(machine, players, change, mailbox);
            this.equivocate = equivocate;
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
                    mailbox.send(new Packet((equivocate.contains(to) ? eq : message), session, machine.phase, vk, to));
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
                SignatureException {

            Map<VerificationKey, EncryptionKey> otherKeys = new HashMap<>();
            otherKeys.putAll(encryptonKeys);

            if (alt != null) {
                otherKeys.put(vk, alt.EncryptionKey());
            }

            Message equivocationCheck = equivocationCheckHash(players, encryptonKeys, newAddresses);
            Message otherCheck = equivocationCheckHash(players, otherKeys, newAddresses);

            if (!equivocationCheckSent) {
                for (VerificationKey to : players.values()) {
                    mailbox.send(new Packet((equivocate.contains(to) ? otherCheck : equivocationCheck), session, machine.phase, vk, to));
                }

                equivocationCheckSent = true;
            }

            // Wait for a similar message from everyone else and check that the result is the name.
            Map<VerificationKey, Message> hashes = mailbox.receiveFromMultipleBlameless(playerSet(1, players.size()), machine.phase);
            hashes.put(vk, equivocationCheck);

            if (areEqual(hashes.values())) {
                if (mailbox.blameReceived()) {
                    fillBlameMatrix(new Matrix());
                }

                return null;
            }

            // If the hashes are not equal, enter the blame phase.
            // Collect all packets from phase 1 and 3.
            machine.phase = Phase.Blame;
            Queue<SignedPacket> evidence = mailbox.getPacketsByPhase(Phase.Announcement);
            evidence.addAll(mailbox.getPacketsByPhase(Phase.BroadcastOutput));
            Message blameMessage = messages.make().attach(Blame.EquivocationFailure(evidence));
            mailbox.broadcast(blameMessage, machine.phase);

            return fillBlameMatrix(new Matrix());
        }
    }

    private final class BroadcastEquivocatorRound extends Round {
        final Set<VerificationKey> equivocate;
        Deque<Address> otherAddresses;

        BroadcastEquivocatorRound(
                Machine machine,
                Map<Integer, VerificationKey> players,
                Address change,
                Mailbox mailbox,
                Set<VerificationKey> equivocate) throws InvalidParticipantSetException {
            super(machine, players, change, mailbox);
            this.equivocate = equivocate;
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
                    mailbox.send(new Packet((equivocate.contains(to) ? otherShuffled : shuffled), session, machine.phase, vk, to));
                }
            } else {
                newAddresses = readNewAddresses(mailbox.receiveFrom(players.get(N), machine.phase));
            }

            return newAddresses;
        }

        @Override
        Matrix equivocationCheck(
                Map<VerificationKey, EncryptionKey> encryptonKeys,
                Queue<Address> newAddresses)
                throws InterruptedException, ValueException,
                FormatException, ProtocolException,
                SignatureException {

            Message equivocationCheck = equivocationCheckHash(players, encryptonKeys, newAddresses);
            Message otherCheck = equivocationCheckHash(players, encryptonKeys, otherAddresses);

            if (!equivocationCheckSent) {
                for (VerificationKey to : players.values()) {
                    mailbox.send(new Packet((equivocate.contains(to) ? otherCheck : equivocationCheck), session, machine.phase, vk, to));
                }

                equivocationCheckSent = true;
            }

            // Wait for a similar message from everyone else and check that the result is the name.
            Map<VerificationKey, Message> hashes = mailbox.receiveFromMultipleBlameless(playerSet(1, players.size()), machine.phase);
            hashes.put(vk, equivocationCheck);

            if (areEqual(hashes.values())) {
                if (mailbox.blameReceived()) {
                    fillBlameMatrix(new Matrix());
                }

                return null;
            }

            // If the hashes are not equal, enter the blame phase.
            // Collect all packets from phase 1 and 3.
            machine.phase = Phase.Blame;
            Queue<SignedPacket> evidence = mailbox.getPacketsByPhase(Phase.Announcement);
            evidence.addAll(mailbox.getPacketsByPhase(Phase.BroadcastOutput));
            Message blameMessage = messages.make().attach(Blame.EquivocationFailure(evidence));
            mailbox.broadcast(blameMessage, machine.phase);

            return fillBlameMatrix(new Matrix());
        }
    }

    // Drop an address during the shuffle phase.
    private class DropAddress extends Round {
        final int drop;

        DropAddress(Machine machine, Map<Integer, VerificationKey> players, Address change, Mailbox mailbox, int drop) throws InvalidParticipantSetException {
            super(machine, players, change, mailbox);
            this.drop = drop;
        }

        @Override
        Message shufflePhase(Message shuffled, Address addr_new) throws InterruptedException, BlameException, SignatureException, ValueException, FormatException {
            Message dropped = messages.make();

            int i = 1;
            while (!shuffled.isEmpty()) {
                if (i != drop) {
                    dropped = dropped.attach(shuffled.readAddress());
                }
                shuffled = shuffled.rest();
                i ++;
            }

            return super.shufflePhase(dropped, addr_new);
        }
    }

    // Drop address and replace it with a duplicate during the Shuffle phase.
    public class DropAddressReplaceDuplicate extends Round {
        final int drop;
        final int replace;

        DropAddressReplaceDuplicate(Machine machine, Map<Integer, VerificationKey> players, Address change, Mailbox mailbox, int drop, int replace) throws InvalidParticipantSetException {
            super(machine, players, change, mailbox);
            this.drop = drop;
            this.replace = replace;
        }

        @Override
        Message shufflePhase(Message shuffled, Address addr_new) throws InterruptedException, BlameException, SignatureException, ValueException, FormatException {
            Message findDuplcate = shuffled;
            shuffled = messages.make();
            Address duplicate = null;

            int i = 1;
            while (!shuffled.isEmpty()) {
                Address address = findDuplcate.readAddress();
                if (i == replace) {
                    duplicate = address;
                }
                shuffled = shuffled.attach(address);
                findDuplcate = findDuplcate.rest();
                i++;
            }

            Message dropped = messages.make();
            i = 1;
            while (!shuffled.isEmpty()) {
                if (i != drop && duplicate != null) {
                    dropped = dropped.attach(shuffled.readAddress());
                } else {
                    dropped = dropped.attach(duplicate);
                }
                shuffled = shuffled.rest();
                i ++;
            }

            return super.shufflePhase(dropped, addr_new);
        }
    }

    // Drop an address and replace it with a new one during the shuffle phase.
    public class DropAddressReplaceNew extends Round {
        final int drop;
        Address replace;

        DropAddressReplaceNew(Machine machine, Map<Integer, VerificationKey> players, Address change, Mailbox mailbox, int drop) throws InvalidParticipantSetException {
            super(machine, players, change, mailbox);
            this.drop = drop;
            replace = crypto.makeSigningKey().VerificationKey().address();
        }

        @Override
        Message shufflePhase(Message shuffled, Address addr_new) throws InterruptedException, BlameException, SignatureException, ValueException, FormatException {
            Message dropped = messages.make();

            int i = 1;
            while (!shuffled.isEmpty()) {
                if (i != drop) {
                    dropped = dropped.attach(shuffled.readAddress());
                } else {
                    dropped = dropped.attach(replace);
                }
                shuffled = shuffled.rest();
                i ++;
            }

            return super.shufflePhase(dropped, addr_new);
        }
    }

    final Phase maliciousPhase;
    final Set<VerificationKey> equivocate;
    final int drop;
    final int duplicate;
    final boolean replaceNew;

    @Override
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

            Round round = null;

            switch (maliciousPhase) {
                case Announcement: {
                    if (equivocate != null) {
                        round = new AnnouncementEquivocatorRound(state, numberedPlayers, change, mailbox, equivocate);
                    }
                    break;
                }
                case Shuffling: {
                    if (drop != 0) {
                        if (duplicate != 0) {
                            round = new DropAddressReplaceDuplicate(state, numberedPlayers, change, mailbox, drop, duplicate);
                        } else if (replaceNew) {
                            round = new DropAddressReplaceNew(state, numberedPlayers, change, mailbox, drop);
                        } else {
                            round = new DropAddress(state, numberedPlayers, change, mailbox, drop);
                        }
                    }
                    break;
                }
                case BroadcastOutput: {
                    if (equivocate != null) {
                        round = new BroadcastEquivocatorRound(state, numberedPlayers, change, mailbox, equivocate);
                    }
                    break;
                }
            }

            if (round == null) {
                round = this.new Round(state, numberedPlayers, change, mailbox);
            }

            round.protocolDefinition();
        } catch (InterruptedException
                | ProtocolException
                | FormatException
                | ValueException
                | InvalidParticipantSetException
                | SignatureException e) {
            state.e = e;
        }

        return state;
    }

    private MaliciousMachine(MessageFactory messages, Crypto crypto,
                             Coin coin, Phase phase,
                             Set<VerificationKey> equivocate,
                             int drop, int duplicate, boolean replaceNew) {
        super(messages, crypto, coin);

        if (phase == null) {
            throw new NullPointerException();
        }

        if (phase != Phase.Announcement && phase != Phase.BroadcastOutput && phase != Phase.Shuffling) {
            throw new IllegalArgumentException();
        }

        if (phase == Phase.Announcement || phase == Phase.BroadcastOutput) {
            if (equivocate == null) {
                throw new NullPointerException();
            }

            if (drop != 0 || duplicate != 0 || replaceNew) {
                throw new IllegalArgumentException();
            }
        }

        if (phase == Phase.Shuffling) {
            if (drop <= 0) {
                throw new IllegalArgumentException();
            }

            if (duplicate > drop) {
                throw new IllegalArgumentException();
            }

            if (replaceNew && duplicate != 0) {
                throw new IllegalArgumentException();
            }
        }

        this.maliciousPhase = phase;
        this.equivocate = equivocate;
        this.drop = drop;
        this.duplicate = duplicate;
        this.replaceNew = replaceNew;
    }

    public static CoinShuffle announcementEquivocator(MessageFactory messages, Crypto crypto, Coin coin, Set<VerificationKey> equivocate) {
        return new MaliciousMachine(messages, crypto, coin, Phase.Announcement, equivocate, 0, 0, false);
    }

    public static CoinShuffle broadcastEquivocator(MessageFactory messages, Crypto crypto, Coin coin, Set<VerificationKey> equivocate) {
        return new MaliciousMachine(messages, crypto, coin, Phase.BroadcastOutput, equivocate, 0, 0, false);
    }

    public static CoinShuffle addressDropper(MessageFactory messages, Crypto crypto, Coin coin, int drop) {
       return new MaliciousMachine(messages, crypto, coin, Phase.Shuffling, null, drop, 0, false);
    }

    public static CoinShuffle addressDropperDuplicator(MessageFactory messages, Crypto crypto, Coin coin, int drop, int duplicate) {
        return new MaliciousMachine(messages, crypto, coin, Phase.Shuffling, null, drop, duplicate, false);
    }

    public static CoinShuffle addressDropperReplacer(MessageFactory messages, Crypto crypto, Coin coin, int drop) {
        return new MaliciousMachine(messages, crypto, coin, Phase.Shuffling, null, drop, 0, true);
    }
}
