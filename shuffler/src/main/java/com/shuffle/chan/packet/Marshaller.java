package com.shuffle.chan.packet;

import com.shuffle.p2p.Bytestring;

/**
 * Represents a way of serializing a class.
 *
 * Created by Daniel Krawisz on 1/31/16.
 */
public interface Marshaller<X> {
    Bytestring marshall(X x);

    X unmarshall(Bytestring string);
}
