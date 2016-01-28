package com.shuffle.protocol.blame;

import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.bitcoin.Signature;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.Packet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Daniel Krawisz on 12/22/15.
 *
 * The matrix represents a set blame accusations from one player to another along with
 * the evidence to prove it.
 */
public class Matrix {
    private static Logger log = LogManager.getLogger(Matrix.class);

    // Who blames who?
    Map<VerificationKey, Map<VerificationKey, Evidence>> blame = new HashMap<>();

    public void put(VerificationKey accuser, VerificationKey accused, Evidence evidence) {
        if (accuser == null || accused == null) {
            throw new NullPointerException();
        }

        if(evidence == null) {
            log.warn("null blame evidence given for " + accuser.toString() + " to " + accused.toString() + " " + Arrays.toString(
            new Throwable().getStackTrace()));
        }

        Map<VerificationKey, Evidence> blames = blame.get(accuser);

        if (blames == null) {
            blames = new HashMap<>();
            blame.put(accuser, blames);
        }

        Evidence blame = blames.get(accused);

        if(blame != null) {
            // There is a warning rather than an exception because I don't know for certain that this should never happen.
            log.warn("Overwriting blame matrix entry [" + accuser.toString() + ", " + accused.toString() + "]");
        }

        blames.put(accused, evidence);
    }

    private static Map<VerificationKey, Evidence> put(Map<VerificationKey, Evidence> to, VerificationKey key, Map<VerificationKey, Evidence> row) {
        if (to == null) {
            return row;
        } else {
            to.putAll(row);
            return to;
        }
    }

    public static void putAll(
            Map<VerificationKey, Map<VerificationKey, Evidence>> to,
            Map<VerificationKey, Map<VerificationKey, Evidence>> bm
    ) {
        for (Map.Entry<VerificationKey, Map<VerificationKey, Evidence>> row : bm.entrySet()) {
            VerificationKey key = row.getKey();
            to.put(key, put(to.get(key), key, row.getValue()));
        }
    }

    // Check whether one player already blamed another for a given offense.
    public boolean blameExists(VerificationKey accuser, VerificationKey accused, Reason reason) {
        Map<VerificationKey, Evidence> blames = blame.get(accuser);

        if (blames == null) {
            return false;
        }

        Evidence offense = blames.get(accused);

        if (offense == null) {
            return false;
        }

        if (offense.reason == reason) {
            return true;
        }

        return false;
    }

    public Evidence get(VerificationKey accuser, VerificationKey accused) {
        Map<VerificationKey, Evidence> accusations = blame.get(accuser);

        if (accusations == null) {
            return null;
        }

        return accusations.get(accused);

    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof Matrix && blame.equals(((Matrix)o).blame);
    }

    @Override
    public int hashCode() {
        return blame.hashCode();
    }

    public boolean match(Matrix bm) {
        if (bm == null) {
            bm = new Matrix();
        }
        Map<VerificationKey, Map<VerificationKey, Evidence>> bml = new HashMap<>();
        putAll(bml, bm.blame);

        for (VerificationKey accuser : blame.keySet()) {
            Map<VerificationKey, Evidence> us = blame.get(accuser);
            Map<VerificationKey, Evidence> them = new HashMap<>();
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

}
