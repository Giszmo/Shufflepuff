package com.shuffle.protocol;

import com.shuffle.cryptocoin.Transaction;
import com.shuffle.cryptocoin.VerificationKey;

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
        EquivocationFailure
    }

    public static class Blame {
        VerificationKey accused = null; // Can be null if we don't know who to accuse yet.
        BlameReason reason;
        Transaction t = null;
        List<Packet> packets;

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
    Map<VerificationKey, Map<VerificationKey, BlameEvidence>> blame = new HashMap<>();

    public void add(VerificationKey accuser, VerificationKey accused, BlameEvidence evidence) {
        Map<VerificationKey, BlameEvidence> blames = blame.get(accuser);

        if (blames == null) {
            blames = new HashMap<>();
            blame.put(accuser, blames);
        }

        blames.put(accused, evidence);
    }
}
