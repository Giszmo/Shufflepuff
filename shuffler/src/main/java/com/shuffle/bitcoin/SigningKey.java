/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.bitcoin;

import com.shuffle.protocol.Packet;
import com.shuffle.protocol.SignedPacket;

/**
 *
 * Should be comparable according to the lexicographic order of the
 * address corresponding to the keys.
 *
 * Created by Daniel Krawisz on 12/3/15.
 */
public abstract class SigningKey implements Comparable {
    public abstract VerificationKey VerificationKey() throws CryptographyError;
    public abstract Signature makeSignature(Transaction t) throws CryptographyError;
    public abstract Signature makeSignature(Packet p) throws CryptographyError;

    public final SignedPacket makeSignedPacket(Packet p) throws CryptographyError {
        return new SignedPacket(p, makeSignature(p));
    }
}
