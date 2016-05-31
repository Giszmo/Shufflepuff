package com.shuffle.chan.packet;

import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.p2p.Bytestring;

/**
 * TODO: I probably don't really need this class.
 *
 * Created by Daniel Krawisz on 5/26/16.
 */
public class VerifyingMarshaller<X> implements Marshaller<Signed<X>> {
    public final VerificationKey key; // The key doing the signing.
    private final Marshaller<X> m;

    public VerifyingMarshaller(VerificationKey key, Marshaller<X> m) {
        this.key = key;
        this.m = m;
    }

    @Override
    public Bytestring marshall(Signed<X> x) {
        return m.marshall(x.message).append(x.signature);
    }

    @Override
    public Signed<X> unmarshall(Bytestring b) {
        if (b == null) return null;

        Bytestring[] stripped = key.verify(b);

        if (stripped == null) return null;

        X x = m.unmarshall(stripped[0]);

        return new Signed<>(x, stripped[1]);
    }
}
