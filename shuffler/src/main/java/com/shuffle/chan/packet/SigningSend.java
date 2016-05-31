package com.shuffle.chan.packet;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.chan.Send;
import com.shuffle.p2p.Bytestring;

/**
 * A Send object that adds a signature to
 *
 * Created by Daniel Krawisz on 4/13/16.
 */
public class SigningSend<X> implements Send<X> {

    // TODO replace this with Send<Bytestring>.
    private final Send<Signed<X>> session;
    private final Marshaller<X> marshaller;
    private final SigningKey key;

    public SigningSend(
            Send<Signed<X>> session,
            Marshaller<X> marshaller,
            SigningKey key) {

        if (session == null || marshaller == null || key == null) throw new NullPointerException();

        this.session = session;
        this.marshaller = marshaller;
        this.key = key;
    }

    @Override
    public boolean send(X x) throws InterruptedException {
        Bytestring b = marshaller.marshall(x);
        if (b == null) return false;
        Bytestring s = key.sign(b);
        return s != null && session.send(new Signed<>(x, s));

    }

    @Override
    public void close() throws InterruptedException {
        session.close();
    }
}
