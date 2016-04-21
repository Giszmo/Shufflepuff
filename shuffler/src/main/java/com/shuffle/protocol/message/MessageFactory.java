/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol.message;

import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.message.Message;

import java.io.IOException;

/**
 * Created by Daniel Krawisz on 12/6/15.
 */
public interface MessageFactory {
    Message make(); // Make a new packet.

    // Receive the next valid packet. Packet must have a valid signature that we recognize that has
    // ALREADY BEEN CHECKED. Throw away all messages that
    Packet receive() throws InterruptedException, // May be thrown if this protocol runs in an interruptable thread.
            IOException; // May be thrown if the internet connection fails.
}
