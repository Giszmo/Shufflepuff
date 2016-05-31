/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol.message;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.p2p.Bytestring;
import com.shuffle.protocol.FormatException;
import com.shuffle.protocol.blame.Blame;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by Daniel Krawisz on 12/19/15.
 */
public interface Message extends Serializable {
    boolean isEmpty();

    Message attach(EncryptionKey ek);

    Message attach(Address addr);

    Message attach(Bytestring sig);

    Message attach(Blame blame);

    EncryptionKey readEncryptionKey() throws FormatException;

    Bytestring readSignature() throws FormatException;

    Address readAddress() throws FormatException;

    Blame readBlame() throws FormatException;

    Message rest() throws FormatException;

    Message hashed();

    // Send across the CoinShuffle network.
    Packet send(Phase phase, VerificationKey to) throws // May be thrown if this protocol runs in an interruptable thread.
            InterruptedException,
            IOException; // May be thrown if the internet connection fails.
}
