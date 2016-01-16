package com.shuffle.moderator;

import com.shuffle.bitcoin.VerificationKey;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * The moderator attempts to get mixes started that are ready to go and passes messages
 * among participants.
 *
 * Created by Daniel Krawisz on 12/26/15.
 */
public class Moderator {
    private final Mixes mixes;
    private final SortedSet<Mixes.Mix> pending = new TreeSet<>();
    private final SortedSet<Mixes.Mix> processing = new TreeSet<>();

    public Moderator(Mixes mixes) {
        this.mixes = mixes;

        long now = System.currentTimeMillis();

        // Get all pending and in progress mixes and put them in the lists.


    }

    public Mixes.Mix createMix(Player player) {
        return null; // TODO
    }

    public boolean register(Mixes.Mix mix, Player player, VerificationKey key) {
        return false; // TODO
    }

    public List<Mixes.Mix> getPendingMixes() {
        return null;
    }

    public List<Mixes.Mix> getPendingMixes(Permission permission) {
        return null;
    }

    public void start() {

    }

    public void stop() {

    }
}
