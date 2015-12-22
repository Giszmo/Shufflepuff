package com.shuffle.protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Daniel Krawisz on 12/22/15.
 */
public class BlameMatrix {
    public class BlameEvidence {
        boolean credible; // Do we believe this evidence?
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
