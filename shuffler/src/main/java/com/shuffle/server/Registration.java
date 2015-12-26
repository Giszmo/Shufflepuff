package com.shuffle.server;

import com.shuffle.protocol.SessionIdentifier;
import com.shuffle.cryptocoin.VerificationKey;

import java.util.List;

/**
 * Represents the registration of a player to a particular mix.
 *
 * Created by Daniel Krawisz on 12/26/15.
 */
public class Registration {

    // The class that actually is stored in the database.
    public class RegistrationDB {
        // An identifier for this player for the purposes of this particular session.
        final int session_id;

        final SessionIdentifier session;

        // The keys corresponding to addresses to be contributed to this join.
        // ( Should be only 1 for the first versions of the protocol.)
        final List<VerificationKey> keys;

        // Whether the player can communicate p2p or must communicate through a websocket.
        final boolean p2p;

        private RegistrationDB(int session_id, SessionIdentifier session, List<VerificationKey> keys, boolean p2p) {
            this.session_id = session_id;

            this.session = session;
            this.keys = keys;
            this.p2p = p2p;
        }
    }

    final RegistrationDB reg;
    final Mix mix;

    public Registration(RegistrationDB reg, Mix mix) {
        this.reg = reg;
        this.mix = mix;
    }
}
