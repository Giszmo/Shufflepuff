/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.moderator;

import com.shuffle.bitcoin.VerificationKey;

import java.util.List;

/**
 * Represents the registration of a player to a particular mix.
 *
 * Created by Daniel Krawisz on 12/26/15.
 */
public interface Registration {

    // The mix that this is a registration for.
    Mixes mix();

    // The time the registration was made.
    long time();

    // The keys corresponding to addresses to be contributed to this join.
    // ( Should be only 1 for the first versions of the protocol.)
    List<VerificationKey> keys();

    // The player corresponding to this registration.
    Player player();
}
