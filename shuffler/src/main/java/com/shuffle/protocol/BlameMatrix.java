package com.shuffle.protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Daniel Krawisz on 12/22/15.
 */
public class BlameMatrix {
    public enum BlameReason {
        InsufficientFunds,
        NoFundsAtAll
    }

    public static class Blame {
        VerificationKey accused = null; // Can be null if we don't know who to accuse yet.
        BlameReason reason;
        Coin.Transaction t = null;

        public Blame(VerificationKey accused, Coin.Transaction t) {
            this.accused = accused;
            this.t = t;
            reason = BlameReason.InsufficientFunds;
        }

        public Blame(VerificationKey accused) {
            this.accused = accused;
            this.reason = BlameReason.NoFundsAtAll;
        }
    }

    public static class BlameEvidence {
        BlameReason reason;
        boolean credible; // Do we believe this evidence?
        Coin.Transaction t = null;

        public BlameEvidence(BlameReason reason, boolean credible) {
            this.reason = reason;
            this.credible = true;
        }

        public BlameEvidence(BlameReason reason, boolean credible, Coin.Transaction t) {
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
