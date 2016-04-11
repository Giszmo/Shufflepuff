/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

import com.shuffle.protocol.message.Message;

/**
 * To be thrown when a message is incorrectly formatted and cannot be interpreted.
 *
 * Created by Daniel Krawisz on 12/4/15.
 */
public class FormatException extends Exception {
    /*public FormatException(Expected expected) {
        this.expected = expected;
    }

    public enum Expected {

    };

    public final Expected expected;
    public final Message message;*/
}
