package com.shuffle.chan.packet;

import com.shuffle.p2p.Bytestring;

/**
 * Created by Daniel Krawisz on 5/24/16.
 */
public class Signed<X> {
    public final X message;
    public final Bytestring signature;

    Signed(X message, Bytestring signature) {
        if (message == null || signature == null) throw new NullPointerException();

        this.message = message;
        this.signature = signature;
    }

    @Override
    public String toString() {
        return "Signed[" + message + ", " + signature + "]";
    }
}
