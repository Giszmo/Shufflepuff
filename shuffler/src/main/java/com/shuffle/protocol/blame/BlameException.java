/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol.blame;

import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.Packet;

/**
 * Created by Daniel Krawisz on 12/4/15.
 *
 * This exception is thrown when malicious behavior is detected on the part of another player.
 */
public final class BlameException extends Exception {
    public final VerificationKey sender;
    public final Packet packet;

    // A blame message history from another party.
    public BlameException(VerificationKey sender, Packet packet) {
        this.sender = sender;
        this.packet = packet;
    }
}
