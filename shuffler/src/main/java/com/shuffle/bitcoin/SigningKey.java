/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.bitcoin;

import com.shuffle.protocol.message.Packet;

/**
 *
 * Should be comparable according to the lexicographic order of the
 * address corresponding to the keys.
 *
 * Created by Daniel Krawisz on 12/3/15.
 */
public interface SigningKey extends Comparable {
    VerificationKey VerificationKey() throws CryptographyError;

    Signature makeSignature(Transaction t) throws CryptographyError;

    Signature makeSignature(Packet p) throws CryptographyError;
}
