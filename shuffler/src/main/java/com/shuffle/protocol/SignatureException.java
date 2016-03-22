/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 1/27/16.
 */
public class SignatureException extends Exception {
    public final SignedPacket packet;

    public SignatureException(SignedPacket packet) {
        if (packet == null) {
            throw new NullPointerException();
        }
        this.packet = packet;
    }
}
