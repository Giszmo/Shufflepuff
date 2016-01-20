package com.shuffle.protocol;

import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.bitcoin.Signature;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;

import java.util.HashMap;
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
        InvalidSignature,
        MissingOutput,
    }

    public static class Blame {
        VerificationKey accused = null; // Can be null if we don't know who to accuse yet.
        BlameReason reason;
        Transaction t = null;
        List<Packet> packets = null;
        DecryptionKey privateKey = null;
        Map<VerificationKey, Signature> invalid = null;

        public Blame(VerificationKey accused, BlameReason reason) {
            if (reason == null) {
                throw new NullPointerException();
            }
            if (reason == BlameReason.InsufficientFunds) {
                throw new NullPointerException();
            }
            this.accused = accused;
            this.reason = reason;
        }

        public Blame(VerificationKey accused, Transaction t, BlameReason reason) {
            if (t == null || accused == null || reason == null) {
                throw new NullPointerException();
            }
            this.accused = accused;
            this.t = t;
            this.reason = reason;
        }

        public Blame(List<Packet> packets) {
            if (packets == null) {
                throw new NullPointerException();
            }
            this.packets = packets;
            this.reason = BlameReason.EquivocationFailure;
        }

        public Blame(DecryptionKey privateKey, List<Packet> packets) {
            this.privateKey = privateKey;
            this.packets = packets;
            this.reason = BlameReason.ShuffleFailure;
        }

        public Blame(Map<VerificationKey, Signature> invalid) {
            this.invalid = invalid;
            this.reason = BlameReason.InvalidSignature;
        }

        @Override
        public String toString() {
            String str = "Blame[";
            if (accused != null) {
                str += (accused.toString() + ", ");
            }
            return str + reason.toString() + "]";
        }
    }

    public static class BlameEvidence {
        BlameReason reason;
        boolean credible; // Do we believe this evidence?
        Transaction t = null;
        Signature signature = null;
        Map<VerificationKey, Message> output = null;
        Map<VerificationKey, EncryptionKey> sent = null;

        public BlameEvidence(BlameReason reason, boolean credible) {
            // A transaction should always be included with InsufficientFunds.
            if (reason == BlameReason.InsufficientFunds) {
                throw new NullPointerException();
            }
            this.reason = reason;
            this.credible = true;
        }

        public BlameEvidence(BlameReason reason, boolean credible, Transaction t) {
            if (t == null) {
                throw new NullPointerException();
            }
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
            String str = reason.toString();
            if (t != null) {
                str += ":" + t.toString();
            }
            return str;
        }
    }

    // Who blames who?
    Map<VerificationKey, Map<VerificationKey, BlameEvidence>> blame = new HashMap<>();

    public void put(VerificationKey accuser, VerificationKey accused, BlameEvidence evidence) {
        Map<VerificationKey, BlameEvidence> blames = blame.get(accuser);

        if (blames == null) {
            blames = new HashMap<>();
            blame.put(accuser, blames);
        }

        blames.put(accused, evidence);
    }

    private static Map<VerificationKey, BlameEvidence> put(Map<VerificationKey, BlameEvidence> to, VerificationKey key, Map<VerificationKey, BlameEvidence> row) {
        if (to == null) {
            return row;
        } else {
            to.putAll(row);
            return to;
        }
    }

    public static void putAll(
            Map<VerificationKey, Map<VerificationKey, BlameEvidence>> to,
            Map<VerificationKey, Map<VerificationKey, BlameEvidence>> bm
    ) {
        for (Map.Entry<VerificationKey, Map<VerificationKey, BlameEvidence>> row : bm.entrySet()) {
            VerificationKey key = row.getKey();
            to.put(key, put(to.get(key), key, row.getValue()));
        }
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
        if (bm == null) {
            bm = new BlameMatrix();
        }
        Map<VerificationKey, Map<VerificationKey, BlameEvidence>> bml = new HashMap<>();
        putAll(bml, bm.blame);

        for (VerificationKey accuser : blame.keySet()) {
            Map<VerificationKey, BlameEvidence> us = blame.get(accuser);
            Map<VerificationKey, BlameEvidence> them = new HashMap<>();
            if (bml.containsKey(accuser)) {
                them.putAll(bml.get(accuser));
            }

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

    static public BlameEvidence EquivocationFailureBroadcast(Map<VerificationKey, Message> output) {
        if (output == null) {
            throw new NullPointerException();
        }

        BlameEvidence evidence = new BlameEvidence();
        evidence.reason = BlameReason.EquivocationFailure;
        evidence.credible = true;
        evidence.output = output;
        return evidence;
    }

    static public BlameEvidence EquivocationFailureAnnouncement(Map<VerificationKey, EncryptionKey> sent) {
        if (sent == null) {
            throw new NullPointerException();
        }

        BlameEvidence evidence = new BlameEvidence();
        evidence.reason = BlameReason.EquivocationFailure;
        evidence.credible = true;
        evidence.sent = sent;
        return evidence;
    }

    static public BlameEvidence InvalidSignature(Signature signature) {
        if (signature == null) {
            throw new NullPointerException();
        }

        BlameEvidence evidence = new BlameEvidence();
        evidence.reason = BlameReason.InvalidSignature;
        evidence.credible = true;
        evidence.signature = signature;
        return evidence;
    }
}
