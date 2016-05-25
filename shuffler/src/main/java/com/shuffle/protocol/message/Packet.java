/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol.message;

import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.p2p.Bytestring;

import java.io.IOException;
import java.io.Serializable;

/**
 * Represents a coin shuffle message with context information attached that is necessary to
 * detect low-level errors. The main protocol does not need to worry about that stuff, so
 * it just uses the Message type for most of what it does.
 *
 * Created by Daniel Krawisz on 12/9/15.
 */
public interface Packet extends Serializable {

    Message payload();
    Phase phase();
    VerificationKey from();
    VerificationKey to();
}
