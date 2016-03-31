/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.bitcoin;

import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.Packet;

/**
 *
 * Should be comparable according to the lexicographic order of the
 * address corresponding to the keys.
 *
 * Created by Daniel Krawisz on 12/3/15.
 */
public interface VerificationKey extends Comparable {
    boolean verify(Transaction t, Signature sig) throws InvalidImplementationError;

    boolean verify(Packet packet, Signature sig);

    boolean equals(Object vk);

    // Get the cryptocurrency address corresponding to this public key.
    Address address();
}
