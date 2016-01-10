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
        DoubleSpend,
        EquivocationFailure,
        ShuffleFailure,
    }

    public static class Blame {
        VerificationKey accused = null; // Can be null if we don't know who to accuse yet.
        BlameReason reason;
        Transaction t = null;
        List<Packet> packets = null;
        DecryptionKey privateKey = null;

        public Blame(VerificationKey accused, Transaction t, BlameReason reason) {
            this.accused = accused;
            this.t = t;
            this.reason = reason;
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

        protected BlameEvidence() {

        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof BlameEvidence)) {
                return false;
            }

            BlameEvidence b = (BlameEvidence)o;

            return reason == b.reason && credible == b.credible &&
                    ((t == null && b.t == null) || (t != null && b.t != null && t.equals(b.t)));
        }

        public boolean match(BlameEvidence e) {
            return equals(e);
        }

        @Override
        public String toString() {
            return reason.toString();
        }
    }

    // Who blames who?
    Map<VerificationKey, Map<VerificationKey, BlameEvidence>> blame = new HashMap<>();

    public void add(VerificationKey accuser, VerificationKey accused, BlameEvidence evidence) {
        Map<VerificationKey, BlameEvidence> blames = blame.get(accuser);

        if (blames == null) {
            blames = new HashMap<>();
            blame.put(accuser, blames);
        }

        blames.put(accused, evidence);
    }

    // Check whether one player already blamed another for a given offense.
    public boolean blameExists(VerificationKey accuser, VerificationKey accused, BlameReason reason) {
        Map<VerificationKey, BlameEvidence> blames = blame.get(accuser);

        if (blames == null) {
            return false;
        }

        BlameEvidence offense = blames.get(accused);

        if (offense == null) {
            return false;
        }

        if (offense.reason == reason) {
            return true;
        }

        return false;
    }

    public BlameEvidence get(VerificationKey accuser, VerificationKey accused) {
        Map<VerificationKey, BlameEvidence> accusations = blame.get(accuser);

        if (accusations == null) {
            return null;
        }

        return accusations.get(accused);

    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BlameMatrix)) {
            return false;
        }

        BlameMatrix bm = (BlameMatrix)o;

        return blame.equals(bm);
    }

    @Override
    public int hashCode() {
        return blame.hashCode();
    }

    public boolean match(BlameMatrix bm) {
        Map<VerificationKey, Map<VerificationKey, BlameEvidence>> bml = new HashMap<>();
        bml.putAll(bm.blame);

        for (VerificationKey accuser : blame.keySet()) {
            Map<VerificationKey, BlameEvidence> us = blame.get(accuser);
            Map<VerificationKey, BlameEvidence> them = new HashMap<>();
            them.putAll(bml.get(accuser));

            for (VerificationKey accused : us.keySet()) {
                if (!us.get(accused).match(them.get(accused))) {
                    return false;
                }

                them.remove(accused);
            }

            if (!them.isEmpty()) {
                return false;
            }

            bml.remove(accuser);
        }

        if (!bml.isEmpty()) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return blame.toString();
    }
}
