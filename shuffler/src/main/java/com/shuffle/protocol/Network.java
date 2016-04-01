/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

import com.shuffle.bitcoin.VerificationKey;

/**
 * A network to the network of shuffle participants.
 *
 * Created by Daniel Krawisz on 12/7/15.
 */
public interface Network {
    void sendTo(VerificationKey to, SignedPacket packet)
            throws InvalidImplementationError, InterruptedException;

    SignedPacket receive()
            throws InvalidImplementationError, InterruptedException, FormatException;
}
