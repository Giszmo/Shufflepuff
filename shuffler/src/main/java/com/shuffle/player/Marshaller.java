/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.player;

import com.shuffle.p2p.Bytestring;
import com.shuffle.protocol.message.Packet;

/**
 * Represents a way of serializing a class.
 *
 * Created by Daniel Krawisz on 1/31/16.
 */
public interface Marshaller {
    Bytestring marshallAndSign(Packet packet);

    Messages.SignedPacket unmarshall(Bytestring string);
}
