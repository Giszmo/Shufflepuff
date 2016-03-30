/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.moderator;

import com.shuffle.bitcoin.Transaction;
import com.shuffle.protocol.SessionIdentifier;

import java.util.List;
import java.util.SortedSet;

/**
 * Created by Daniel Krawisz on 12/25/15.
 */
public interface Mixes {
    public enum MixOutcome {
        Pending,
        Running,
        Success,
        Fail,
        Unknown
    }

    public interface Mix {

        // The specific protocol and version to be run.
        SessionIdentifier session();

        long amount(); // The amount to be mixed per person.

        // The time at which the protocol can begin.
        long minTime();

        // The time at which the protocol fails.
        long maxTime();

        // The time that the mix was registered.
        long registrationTime();

        // The minimum number of players allowed to
        int minPlayers();

        // The maximum number of players allowed to join.
        int maxPlayers();

        // The number of times a mix can be retried before it fails.
        int retries();

        SortedSet<Registration> registered();

        // Who can view and join this mix.
        Permission permission();
        void setPermission(Permission permission);

        // Whether the mix was completed successfully.
        MixOutcome outcome();

        // The transaction of this mix if it was successfully completed and if we know what it is.
        // Can be null.
        Transaction transaction();

        // Register a player for a mix. Can return null if the player is not allowed to join.
        Registration register(Player player);
    }

    public interface MixQuery {
        MixQuery outcome(MixOutcome outcome);

        // All mixes that this player has permission to see.
        MixQuery permission(Player player);

        List<Mixes> execute();
    }

    MixQuery get();
    Mix create(long amount, long minTime, long maxTime, int minPlayers, int maxPlayers, int retries);
}
