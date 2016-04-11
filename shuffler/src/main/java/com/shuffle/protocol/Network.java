/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.message.Packet;

import java.io.IOException;

/**
 * A network to the network of shuffle participants.
 *
 * Created by Daniel Krawisz on 12/7/15.
 */
public interface Network {
    void sendTo(VerificationKey to, Packet packet)
            // May be thrown by an implementation if it enters an illegal state.
            throws InvalidImplementationError,
            // May be thrown if this protocol runs in an interruptable thread.
            InterruptedException,
            IOException; // May be thrown if the internet connection fails.

    // Receive the next valid packet. Packet must have a valid signature that we recognize that has
    // ALREADY BEEN CHECKED. Throw away all messages that
    Packet receive()
            // May be thrown by in implementation that enters an illegal state.
            throws InvalidImplementationError,
            InterruptedException, // May be thrown if this protocol runs in an interruptable thread.
            IOException; // May be thrown if the internet connection fails.
}
