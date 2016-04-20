/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;

/**
 * A simple wrapper for []byte.
 *
 * Created by Daniel Krawisz on 1/19/16.
 */

import java.io.Serializable;
import java.util.Arrays;

/**
 * A simple wrapper for []byte.
 *
 * Created by Daniel Krawisz on 12/19/15.
 */
public class Bytestring implements Serializable {
    public final byte[] bytes;

    public Bytestring(byte[] bytes) {
        this.bytes = bytes;
    }

    Bytestring prepend(byte[] pre) {
        byte[] newBytes = new byte[bytes.length + pre.length];

        int i;
        for (i = 0; i < pre.length; i++) {
            newBytes[i] = pre[i];
        }

        for (byte aByte : bytes) {
            newBytes[i] = aByte;
            i++;
        }

        return new Bytestring(newBytes);
    }

    Bytestring append(byte[] post) {
        byte[] newBytes = new byte[bytes.length + post.length];

        int i;
        for (i = 0; i < bytes.length; i++) {
            newBytes[i] = post[i];
        }

        for (int j = 0; j < post.length; j++) {
            newBytes[i] = bytes[j];
            i++;
        }

        return new Bytestring(newBytes);
    }

    public Bytestring xor(Bytestring b) {
        if (bytes.length != b.bytes.length) {
            return null;
        }

        byte[] newBytes = new byte[bytes.length];

        for (int i = 0; i < bytes.length; i++) {
            newBytes[i] = (byte)(bytes[i] ^ b.bytes[i]);
        }

        return new Bytestring(newBytes);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Bytestring)) {
            return false;
        }

        Bytestring b = (Bytestring)o;

        if (b.bytes.length != bytes.length) {
            return false;
        }

        for (int i = 0; i < bytes.length; i ++) {
            if (bytes[i] != b.bytes[i]) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int code = 0;
        for (int i : bytes) {
            code += i;
        }

        return code;
    }

    @Override
    public String toString() {
        return Arrays.toString(bytes);
    }
}
