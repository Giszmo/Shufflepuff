package com.shuffle.protocol;

import com.shuffle.cryptocoin.DecryptionKey;
import com.shuffle.cryptocoin.Transaction;
import com.shuffle.cryptocoin.VerificationKey;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Daniel Krawisz on 12/22/15.
 */
public class BlameMatrix {
    public enum BlameReason {
        InsufficientFunds,
        NoFundsAtAll,
        EquivocationFailure,
        ShuffleFailure,
    }

    public static class Blame {
        VerificationKey accused = null; // Can be null if we don't know who to accuse yet.
        BlameReason reason;
        Transaction t = null;
        List<Packet> packets = null;
        DecryptionKey privateKey = null;

        public Blame(VerificationKey accused, Transaction t) {
            this.accused = accused;
            this.t = t;
            reason = BlameReason.InsufficientFunds;
        }

        public Blame(VerificationKey accused) {
            this.accused = accused;
            this.reason = BlameReason.NoFundsAtAll;
        }

        public Blame(List<Packet> packets) {
            this.packets = packets;
            this.reason = BlameReason.EquivocationFailure;
        }

        public Blame(DecryptionKey privateKey, List<Packet> packets) {
            this.privateKey = privateKey;
            this.packets = packets;
            this.reason = BlameReason.ShuffleFailure;
        }
    }

    public static class BlameEvidence {
        BlameReason reason;
        boolean credible; // Do we believe this evidence?
        Transaction t = null;

        public BlameEvidence(BlameReason reason, boolean credible) {
            this.reason = reason;
            this.credible = true;
        }

        public BlameEvidence(BlameReason reason, boolean credible, Transaction t) {
            this.reason = reason;
            this.credible = true;
            this.t = t;
        }
    }

    // Who blames who?
    Map<VerificationKey, Map<VerificationKey, List<BlameEvidence>>> blame = new HashMap<>();

    public void add(VerificationKey accuser, VerificationKey accused, BlameEvidence evidence) {
        Map<VerificationKey, List<BlameEvidence>> blames = blame.get(accuser);

        if (blames == null) {
            blames = new HashMap<>();
            blame.put(accuser, blames);
        }

        List<BlameEvidence> offenses = blames.get(accused);

        if (offenses == null) {
            offenses = new LinkedList<>();
            blames.put(accused, offenses);
        }

        offenses.add(evidence);
    }

    // Check whether one player already blamed another for a given offense.
    public boolean blameExists(VerificationKey accuser, VerificationKey accused, BlameReason reason) {
        Map<VerificationKey, List<BlameEvidence>> blames = blame.get(accuser);

        if (blames == null) {
            return false;
        }

        List<BlameEvidence> offenses = blames.get(accused);

        if (offenses == null) {
            return false;
        }

        for (BlameEvidence evidence : offenses) {
            if (evidence.reason == reason) {
                return true;
            }
        }

        return false;
    }
}
