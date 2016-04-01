/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol.blame;

import com.shuffle.bitcoin.VerificationKey;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Daniel Krawisz on 12/22/15.
 *
 * The matrix represents a set blame accusations from one player to another along with
 * the evidence to prove it.
 */
public class Matrix extends Throwable {
    private static final Logger log = LogManager.getLogger(Matrix.class);

    // Who blames who?
    final Map<VerificationKey, Map<VerificationKey, Evidence>> blame = new HashMap<>();

    // Put a new entry in the blame matrix.
    public void put(VerificationKey accuser, Evidence evidence) {
        if (accuser == null || evidence == null) {
            throw new NullPointerException();
        }

        VerificationKey accused = evidence.accused;

        Map<VerificationKey, Evidence> blames = blame.get(accuser);

        if (blames == null) {
            blames = new HashMap<>();
            blame.put(accuser, blames);
        }

        Evidence blame = blames.get(accused);

        if (blame != null) {
            log.error("Overwriting blame matrix entry "
                    + accused + " => " + blame + " with " + evidence);
            throw new IllegalArgumentException();
        }

        blames.put(accused, evidence);
    }

    private static Map<VerificationKey, Evidence> put(
            Map<VerificationKey, Evidence> to,
            Map<VerificationKey, Evidence> row) {
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
            to.put(key, put(to.get(key), row.getValue()));
        }
    }

    // Check whether one player already blamed another for a given offense.
    public boolean blameExists(VerificationKey accuser, VerificationKey accused, Reason reason) {
        Map<VerificationKey, Evidence> blames = blame.get(accuser);

        if (blames == null) {
            return false;
        }

        Evidence offense = blames.get(accused);

        return offense != null && offense.reason == reason;

    }

    public Evidence get(VerificationKey accuser, VerificationKey accused) {
        Map<VerificationKey, Evidence> accusations = blame.get(accuser);

        if (accusations == null) {
            return null;
        }

        return accusations.get(accused);

    }

    public boolean isEmpty() {
        if (blame.isEmpty()) return true;

        for (Map<VerificationKey, Evidence> row : blame.values()) {
            if (!row.isEmpty()) return false;
        }

        return true;
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

        return bml.isEmpty();

    }

    @Override
    public String toString() {
        return blame.toString();
    }

}
