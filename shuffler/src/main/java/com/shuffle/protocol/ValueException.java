/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/7/15.
 */
public class ValueException extends Exception {
    public enum Values{session, phase, sender, recipient}

    Values value;
    String expected;
    String found;

    ValueException(Values value, String expected, String found) {
        this.value = value;
        this.expected = expected;
        this.found = found;
    }

    ValueException(Values value) {
        this.value = value;
        this.expected = null;
        this.found = null;
    }
}
