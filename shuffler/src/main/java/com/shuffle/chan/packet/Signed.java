package com.shuffle.chan.packet;

import com.shuffle.p2p.Bytestring;

import java.io.Serializable;

/**
 * Created by Daniel Krawisz on 5/24/16.
 */
public class Signed<X> implements Serializable {
    public final X message;
    public final Bytestring signature;

    Signed(X message, Bytestring signature) {
        if (message == null || signature == null) throw new NullPointerException();

        this.message = message;
        this.signature = signature;
    }

    @Override
    public String toString() {
        return "Sig[" + message + ", " + signature + "]";
    }

    @Override
    public int hashCode() {
        return message.hashCode() + 17 * signature.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Signed)) return false;

        Signed x = (Signed)o;

        return message.equals(x.message) && signature.equals(x.signature);
    }
}
