package com.shuffle.p2p;

/**
 * A simple wrapper for []byte.
 *
 * Created by Daniel Krawisz on 1/19/16.
 */
public interface Bytestring {

    byte[] getBytes();

    /*private byte[] bytes;

    public Bytestring(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getBytes(){
        return bytes;
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

        bytes = newBytes;
        return this;
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

        bytes = newBytes;
        return this;
    }

    // Strip the first n bytes and return the stripped bytes.
    byte[] strip(int n) {
        byte[] stripped = new byte[n];
        int N = (n > bytes.length ? bytes.length : n);

        int i, j;
        for (i = 0; i < N; i ++) {
            stripped[i] = bytes[i];
        }

        // Fill the rest with zeros.
        for (j = i; j < n; j ++) {
            stripped[j] = 0;
        }

        byte[] newBytes = new byte[bytes.length - i];
        j = 0;
        while(i < bytes.length) {
            newBytes[j] = bytes[i];
            j++;
            i++;
        }

        bytes = newBytes;

        return stripped;
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
    }*/
}
