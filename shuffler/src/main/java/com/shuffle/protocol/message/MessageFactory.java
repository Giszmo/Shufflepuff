/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol.message;

import com.shuffle.protocol.message.Message;

/**
 * Created by Daniel Krawisz on 12/6/15.
 */
public interface MessageFactory {
    Message make(); // Make a new packet.
}
