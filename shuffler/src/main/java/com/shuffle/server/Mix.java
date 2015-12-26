package com.shuffle.server;

import com.shuffle.protocol.SessionIdentifier;

import java.io.Serializable;

/**
 * Created by Daniel Krawisz on 12/25/15.
 */
public class Mix {
    public enum MixOutcome {
        Pending,
        Success,
        Fail,
        Unknown
    }

    // The data that is included in the database.
    //
    // The parameters of the mix cannot be altered after the entry is created because people
    // who join have to be sure they know what they are agreeing to.
    public final class MixDB implements Serializable {
        final long id;

        // The specific protocol and version to be run.
        final SessionIdentifier identifier;

        final long amount; // The amount to be mixed per person.

        // The time at which the protocol can begin.
        final long minTime;

        // The time at which the protocol fails.
        final long maxTime;

        // The minimum number of players allowed to
        final int minPlayers;

        // The maximum number of players allowed to join.
        final int maxPlayers;

        // The number of times a mix can be retried before it fails.
        final int retries;

        // Who can view and join this mix.
        final Permission permission;

        private MixDB(long id, SessionIdentifier identifier, long amount, long minTime, long maxTime, int minPlayers, int maxPlayers, int retries, Permission permission) {

            this.id = id;
            this.identifier = identifier;
            this.amount = amount;
            this.minTime = minTime;
            this.maxTime = maxTime;
            this.minPlayers = minPlayers;
            this.maxPlayers = maxPlayers;
            this.retries = retries;
            this.permission = permission;
        }
    }

    final Database db;
    final MixDB mix;

    // Whether the mix was completed successfully.
    MixOutcome outcome = MixOutcome.Pending;

    // The transaction id of this mix if it was successfully completed and if we know what it is.
    // Can be null.
    byte[] transactionID = null;

    public Mix(Database db, MixDB mix) {
        this.db = db;
        this.mix = mix;
    }
}
