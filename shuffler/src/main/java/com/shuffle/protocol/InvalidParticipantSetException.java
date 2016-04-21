/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

import com.shuffle.bitcoin.VerificationKey;

import java.util.Map;

/**
 * Created by Daniel Krawisz on 12/7/15.
 */
public class InvalidParticipantSetException extends Exception {
    final VerificationKey vk;
    final Map<Integer, VerificationKey> players;

    public InvalidParticipantSetException(VerificationKey vk, Map<Integer, VerificationKey> players) {
        this.vk = vk;
        this.players = players;
    }
}
