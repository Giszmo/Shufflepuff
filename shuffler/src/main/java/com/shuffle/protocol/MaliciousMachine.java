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
import com.shuffle.protocol.blame.Blame;
import com.shuffle.protocol.blame.BlameException;
import com.shuffle.protocol.blame.Matrix;
import com.shuffle.protocol.message.Message;
import com.shuffle.protocol.message.MessageFactory;
import com.shuffle.protocol.message.Packet;
import com.shuffle.protocol.message.Phase;

import java.io.IOException;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
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
public final class MaliciousMachine extends CoinShuffle {

    private final class AnnouncementEquivocatorRound extends Round {
        final Set<VerificationKey> equivocate;
        DecryptionKey alt = null;

        AnnouncementEquivocatorRound(
                CurrentPhase machine,
                long amount,
                SigningKey sk,
                Map<Integer, VerificationKey> players,
                Address addrNew,
                Address change,
                Mailbox mailbox,
                Set<VerificationKey> equivocate) throws InvalidParticipantSetException {
            super(machine, amount, sk, players, addrNew, change, mailbox);
            this.equivocate = equivocate;
        }

        @Override
        final DecryptionKey broadcastNewKey(Map<VerificationKey, Address> changeAddresses) throws IOException, InterruptedException {

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
                    mailbox.send(
                            (equivocate.contains(to) ? eq : message).prepare(phase.get(), to));
                }
            }
            return dk;
        }

        @Override
        final void equivocationCheck(
                Map<VerificationKey, EncryptionKey> encryptonKeys,
                Queue<Address> newAddresses,
                boolean errorCase)
                throws WaitingException, Matrix, InterruptedException, FormatException, IOException {

            Map<VerificationKey, EncryptionKey> otherKeys = new HashMap<>();
            otherKeys.putAll(encryptonKeys);

            if (alt != null) {
                otherKeys.put(vk, alt.EncryptionKey());
            }

            Message equivocationCheck = equivocationCheckHash(players, encryptonKeys, newAddresses);
            Message otherCheck = equivocationCheckHash(players, otherKeys, newAddresses);

            for (VerificationKey to : players.values()) {
                mailbox.send((equivocate.contains(to) ? otherCheck : equivocationCheck).prepare(
                        phase.get(), to));
            }

            // Wait for a similar message from everyone else and check that the result is the name.
            Map<VerificationKey, Message> hashes = mailbox.receiveFromMultipleBlameless(
                    playerSet(1, players.size()), phase.get());
            hashes.put(vk, equivocationCheck);

            if (areEqual(hashes.values())) {
                if (mailbox.blame() || errorCase) {
                    blameBroadcastShuffleMessages();
                }
            }

            // If the hashes are not equal, enter the blame phase.
            // Collect all packets from phase 1 and 3.
            phase.set(Phase.Blame);
            Queue<Packet> evidence = mailbox.getPacketsByPhase(Phase.Announcement);
            evidence.addAll(mailbox.getPacketsByPhase(Phase.BroadcastOutput));
            Message blameMessage = messages.make().attach(Blame.EquivocationFailure(evidence));
            mailbox.broadcast(blameMessage, phase.get());

            throw fillBlameMatrix();
        }
    }

    private final class BroadcastEquivocatorRound extends Round {
        final Set<VerificationKey> equivocate;
        Deque<Address> otherAddresses;

        BroadcastEquivocatorRound(
                CurrentPhase machine,
                long amount,
                SigningKey sk,
                Map<Integer, VerificationKey> players,
                Address addrNew,
                Address change,
                Mailbox mailbox,
                Set<VerificationKey> equivocate) throws InvalidParticipantSetException {
            super(machine, amount, sk, players, addrNew, change, mailbox);
            this.equivocate = equivocate;
        }

        @Override
        final Deque<Address> readAndBroadcastNewAddresses(Message shuffled)
                throws FormatException, InterruptedException,
                BlameException, WaitingException, IOException {

            Deque<Address> newAddresses;
            if (me == N) {
                // Need to make sure that the other messages are actually different.
                Message otherShuffled = shuffled;
                List<Message> equivocation;
                do {
                    equivocation = new LinkedList<>();
                    otherShuffled = shuffle(otherShuffled);
                    equivocation.add(shuffled);
                    equivocation.add(otherShuffled);
                } while (areEqual(equivocation));

                newAddresses = readNewAddresses(shuffled);
                otherAddresses = readNewAddresses(otherShuffled);

                for (VerificationKey to : players.values()) {
                    mailbox.send((equivocate.contains(to) ? otherShuffled : shuffled).prepare(
                            phase.get(), to));
                }
            } else {
                newAddresses = readNewAddresses(mailbox.receiveFrom(players.get(N), phase.get()));
            }

            return newAddresses;
        }

        @Override
        final void equivocationCheck(
                Map<VerificationKey, EncryptionKey> encryptonKeys,
                Queue<Address> newAddresses,
                boolean errorCase)
                throws InterruptedException,
                FormatException, IOException,
                WaitingException, Matrix {

            Message equivocationCheck = equivocationCheckHash(players, encryptonKeys, newAddresses);
            Message otherCheck = equivocationCheckHash(players, encryptonKeys, otherAddresses);

            for (VerificationKey to : players.values()) {
                mailbox.send((equivocate.contains(to) ? otherCheck : equivocationCheck).prepare(
                        phase.get(), to));
            }

            // Wait for a similar message from everyone else and check that the result is the name.
            Map<VerificationKey, Message> hashes = mailbox.receiveFromMultipleBlameless(
                    playerSet(1, players.size()), phase.get());
            hashes.put(vk, equivocationCheck);

            if (areEqual(hashes.values())) {
                if (mailbox.blame() || errorCase) {
                    blameBroadcastShuffleMessages();
                }
            }

            // If the hashes are not equal, enter the blame phase.
            // Collect all packets from phase 1 and 3.
            phase.set(Phase.Blame);
            Queue<Packet> evidence = mailbox.getPacketsByPhase(Phase.Announcement);
            evidence.addAll(mailbox.getPacketsByPhase(Phase.BroadcastOutput));
            Message blameMessage = messages.make().attach(Blame.EquivocationFailure(evidence));
            mailbox.broadcast(blameMessage, phase.get());

            throw fillBlameMatrix();
        }
    }

    // Drop an address during the shuffle phase.
    private class DropAddress extends Round {
        final int drop;

        DropAddress(
                CurrentPhase machine,
                long amount,
                SigningKey sk,
                Map<Integer, VerificationKey> players,
                Address addrNew,
                Address change, Mailbox mailbox, int drop) throws InvalidParticipantSetException {
            super(machine, amount, sk, players, addrNew, change, mailbox);
            this.drop = drop;
        }

        @Override
        Message shufflePhase(Message shuffled, Address addrNew)
                throws FormatException {
            Message dropped = messages.make();

            int i = 1;
            while (!shuffled.isEmpty()) {
                if (i != drop) {
                    dropped = dropped.attach(shuffled.readAddress());
                }
                shuffled = shuffled.rest();
                i ++;
            }

            return super.shufflePhase(dropped, addrNew);
        }
    }

    // Drop address and replace it with a duplicate during the Shuffle phase.
    public class DropAddressReplaceDuplicate extends Round {
        final int drop;
        final int replace;

        DropAddressReplaceDuplicate(
                CurrentPhase machine,
                long amount,
                SigningKey sk,
                Map<Integer, VerificationKey> players,
                Address addrNew, Address change,
                Mailbox mailbox, int drop, int replace) throws InvalidParticipantSetException {
            super(machine, amount, sk, players, addrNew, change, mailbox);
            this.drop = drop;
            this.replace = replace;
        }

        @Override
        Message shufflePhase(Message shuffled, Address addrNew)
                throws FormatException {
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

            return super.shufflePhase(dropped, addrNew);
        }
    }

    // Drop an address and replace it with a new one during the shuffle phase.
    public class DropAddressReplaceNew extends Round {
        final int drop;
        Address replace;

        DropAddressReplaceNew(
                CurrentPhase machine,
                long amount,
                SigningKey sk,
                Map<Integer, VerificationKey> players,
                Address addrNew,
                Address change, Mailbox mailbox, int drop) throws InvalidParticipantSetException {
            super(machine, amount, sk, players, addrNew, change, mailbox);
            this.drop = drop;
            replace = crypto.makeSigningKey().VerificationKey().address();
        }

        @Override
        Message shufflePhase(Message shuffled, Address addrNew)
                throws FormatException {
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

            return super.shufflePhase(dropped, addrNew);
        }
    }

    public final class DoubleSpender extends Round {
        final Transaction t;
        boolean spent = false;

        DoubleSpender(
                CurrentPhase machine,
                long amount,
                SigningKey sk,
                Map<Integer, VerificationKey> players,
                Address addrNew,
                Address change, Mailbox mailbox, Transaction t
        ) throws InvalidParticipantSetException {
            super(machine, amount, sk, players, addrNew, change, mailbox);
            this.t = t;
        }

        @Override
        // This is when we maliciously double spend the transaction.
        final void equivocationCheck(
                Map<VerificationKey, EncryptionKey> encryptonKeys,
                Queue<Address> newAddresses,
                boolean errorCase)
                throws InterruptedException,
                FormatException, IOException,
                WaitingException, Matrix {

            if (!spent) {
                try {
                    t.send();
                    spent = true;
                } catch (CoinNetworkException e) {
                    // TODO
                }
            }

            super.equivocationCheck(encryptonKeys, newAddresses, errorCase);
        }

        @Override
        // We made the double spend transaction so obviously we're not going to check honestly.
        final void checkDoubleSpending(Transaction t) {

        }
    }

    private final Phase maliciousPhase;
    private final Set<VerificationKey> equivocate;
    private final int drop;
    private final int duplicate;
    private final boolean replaceNew;
    private final Transaction t;

    @Override
    public Transaction runProtocol(
            long amount, // The amount to be shuffled per player.
            SigningKey sk, // The signing key of the current player.
            // The set of players, sorted alphabetically by address.
            SortedSet<VerificationKey> players,
            Address addrNew,
            Address change, // Change address. (can be null)
            // If this is not null, the machine is put in this channel so that another thread can
            // query the phase as it runs.
            Send<Phase> chan
    ) throws WaitingException,
            InvalidParticipantSetException,
            InterruptedException, FormatException,
            IOException, CoinNetworkException, Matrix {

        if (amount <= 0) {
            throw new IllegalArgumentException();
        }
        if (sk == null || players == null ) {
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
                sk.VerificationKey(), numberedPlayers.values(), messages
        );

        Round round = null;

        switch (maliciousPhase) {
            case Announcement: {
                if (equivocate != null) {
                    round = new AnnouncementEquivocatorRound(
                            machine, amount, sk, numberedPlayers, addrNew, change, mailbox, equivocate);
                }
                break;
            }
            case Shuffling: {
                if (drop != 0) {
                    if (duplicate != 0) {
                        round = new DropAddressReplaceDuplicate(
                                machine, amount, sk,
                                numberedPlayers, addrNew, change, mailbox, drop, duplicate
                        );
                    } else if (replaceNew) {
                        round = new DropAddressReplaceNew(
                                machine, amount, sk, numberedPlayers, addrNew, change, mailbox, drop
                        );
                    } else {
                        round = new DropAddress(
                                machine, amount, sk, numberedPlayers, addrNew, change, mailbox, drop
                        );
                    }
                }
                break;
            }
            case BroadcastOutput: {
                if (equivocate != null) {
                    round = new BroadcastEquivocatorRound(
                            machine, amount, sk,
                            numberedPlayers, addrNew, change, mailbox, equivocate
                    );
                }
                break;
            }
            case VerificationAndSubmission: {
                round = new DoubleSpender(
                        machine, amount, sk, numberedPlayers, addrNew, change, mailbox, t
                );
                break;
            }
            default: { }
        }

        if (round == null) {
            round = this.new Round(machine, amount, sk, numberedPlayers, addrNew, change, mailbox);
        }

        try {
            return round.protocolDefinition();
        } catch (CoinNetworkException e) {
            // If we are a double spender, we can try to spend our own transaction and get
            // an exception as a result, which we catch here.
            return e.t;
        }
    }

    private MaliciousMachine(MessageFactory messages, Crypto crypto,
                             Coin coin, Phase phase,
                             Set<VerificationKey> equivocate,
                             int drop, int duplicate, boolean replaceNew, Transaction t) {
        super(messages, crypto, coin);

        // First check for valid input values.
        if (phase == null) {
            throw new NullPointerException();
        }

        switch (phase) {
            case Announcement:
            case BroadcastOutput: {
                if (equivocate == null) {
                    throw new NullPointerException();
                }

                if (drop != 0 || duplicate != 0 || replaceNew) {
                    throw new IllegalArgumentException();
                }

                if (t != null) {
                    throw new IllegalArgumentException();
                }

                break;
            }
            case Shuffling: {
                if (drop <= 0) {
                    throw new IllegalArgumentException();
                }

                if (duplicate > drop) {
                    throw new IllegalArgumentException();
                }

                if (replaceNew && duplicate != 0) {
                    throw new IllegalArgumentException();
                }

                if (t != null) {
                    throw new IllegalArgumentException();
                }

                break;
            }
            case VerificationAndSubmission: {
                if (t == null) {
                    throw new IllegalArgumentException();
                }
                break;
            }
            default: {
                throw new IllegalArgumentException();
            }
        }

        this.maliciousPhase = phase;
        this.equivocate = equivocate;
        this.drop = drop;
        this.duplicate = duplicate;
        this.replaceNew = replaceNew;
        this.t = t;
    }

    public static CoinShuffle announcementEquivocator(
            MessageFactory messages, Crypto crypto, Coin coin, Set<VerificationKey> equivocate) {
        return new MaliciousMachine(
                messages, crypto, coin, Phase.Announcement, equivocate, 0, 0, false, null);
    }

    public static CoinShuffle broadcastEquivocator(
            MessageFactory messages, Crypto crypto, Coin coin, Set<VerificationKey> equivocate) {
        return new MaliciousMachine(
                messages, crypto, coin, Phase.BroadcastOutput, equivocate, 0, 0, false, null);
    }

    public static CoinShuffle addressDropper(
            MessageFactory messages, Crypto crypto, Coin coin, int drop) {
        return new MaliciousMachine(
                messages, crypto, coin, Phase.Shuffling, null, drop, 0, false, null);
    }

    public static CoinShuffle addressDropperDuplicator(
            MessageFactory messages, Crypto crypto, Coin coin, int drop, int duplicate) {
        return new MaliciousMachine(
                messages, crypto, coin, Phase.Shuffling, null, drop, duplicate, false, null);
    }

    public static CoinShuffle addressReplacer(
            MessageFactory messages, Crypto crypto, Coin coin, int drop) {
        return new MaliciousMachine(
                messages, crypto, coin, Phase.Shuffling, null, drop, 0, true, null);
    }

    public static CoinShuffle doubleSpender(
            MessageFactory messages, Crypto crypto, Coin coin, Transaction t) {
        return new MaliciousMachine(
                messages, crypto, coin, Phase.VerificationAndSubmission, null, 0, 0, false, t);
    }
}
