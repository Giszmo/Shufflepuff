/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol.message;

import com.shuffle.bitcoin.VerificationKey;

import java.io.IOException;

/**
 * Represents a coin shuffle message with context information attached that is necessary to
 * detect low-level errors. The main protocol does not need to worry about that stuff, so
 * it just uses the Message type for most of what it does.
 *
 * Created by Daniel Krawisz on 12/9/15.
 */
public interface Packet {

    Message payload();
    Phase phase();
    VerificationKey from();
    VerificationKey to();

    // Send across the CoinShuffle network.
    void send() throws // May be thrown if this protocol runs in an interruptable thread.
            InterruptedException,
            IOException; // May be thrown if the internet connection fails.
}
