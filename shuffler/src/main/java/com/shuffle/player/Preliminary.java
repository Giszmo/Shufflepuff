package com.shuffle.player;

import com.shuffle.bitcoin.VerificationKey;

import java.util.SortedSet;

/**
 * Created by Daniel Krawisz on 4/26/16.
 */
public class Preliminary {

    // Who will be in this round?
    public final SortedSet<VerificationKey> players;

    public Preliminary(SortedSet<VerificationKey> players) {
        if (players == null) throw new NullPointerException();

        this.players = players;
    }
}
