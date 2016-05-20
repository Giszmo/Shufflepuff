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
import java.util.LinkedList;
import java.util.List;

/**
 * A wrapper for []byte.
 *
 * Created by Daniel Krawisz on 12/19/15.
 */
public class Bytestring implements Serializable {
    public final byte[] bytes;

    public Bytestring(byte[] bytes) {
        this.bytes = bytes;
    }

    public Bytestring prepend(Bytestring pre) {
        byte[] newBytes = new byte[bytes.length + pre.bytes.length];

        int i;
        for (i = 0; i < pre.bytes.length; i++) {
            newBytes[i] = pre.bytes[i];
        }

        for (byte aByte : bytes) {
            newBytes[i] = aByte;
            i++;
        }

        return new Bytestring(newBytes);
    }

    public Bytestring append(Bytestring post) {
        byte[] newBytes = new byte[bytes.length + post.bytes.length];

        int i;
        for (i = 0; i < bytes.length; i++) {
            newBytes[i] = bytes[i];
        }

        for (int j = 0; j < post.bytes.length; j++) {
            newBytes[i] = post.bytes[j];
            i++;
        }

        return new Bytestring(newBytes);
    }

    private Bytestring cp9(int last, int next, int i) {

        byte[] b = new byte[next - last];
        for (int j = 0; j < b.length; j ++) {
            b[j] = bytes[i];

            i ++;
        }

        return new Bytestring(b);
    }

    public Bytestring[] chop(int[] where) {

        List<Bytestring> l = new LinkedList<>();
        int last = 0;
        int i = 0;

        for (int next : where) {
            if (next < last) continue;

            if (next > bytes.length) next = bytes.length;

            Bytestring section = cp9(last, next, i);
            l.add(section);
            i += section.bytes.length;

            last = next;
        }

        if (i < bytes.length) {
            l.add(cp9(last, bytes.length, i));
        } else {
            l.add(new Bytestring(new byte[]{}));
        }

        return l.toArray(new Bytestring[l.size()]);
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
