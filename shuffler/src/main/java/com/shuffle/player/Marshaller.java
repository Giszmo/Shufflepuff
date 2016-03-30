/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.player;

import com.shuffle.protocol.FormatException;
import com.shuffle.protocol.SignedPacket;

/**
 * Represents a way of serializing a class.
 *
 * Created by Daniel Krawisz on 1/31/16.
 */
public interface Marshaller<Format> {
    Format marshall(SignedPacket packet);

    SignedPacket unmarshall(Format string) throws FormatException;
}
