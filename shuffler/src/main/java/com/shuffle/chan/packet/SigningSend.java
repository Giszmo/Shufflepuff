package com.shuffle.chan.packet;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.chan.Send;
import com.shuffle.p2p.Bytestring;

/**
 * Created by Daniel Krawisz on 4/13/16.
 */
public class SigningSend<X> implements Send<X> {

    /**
     * Represents a way of serializing a class.
     *
     * Created by Daniel Krawisz on 1/31/16.
     */
    public interface Marshaller<X> {
        Bytestring marshall(X x);

        X unmarshall(Bytestring string);
    }

    private final Send<Bytestring> session;
    private final Marshaller<X> marshaller;
    private final SigningKey key;

    public SigningSend(
            Send<Bytestring> session,
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
        return b != null && session.send(b.append(key.sign(b)));

    }

    @Override
    public void close() throws InterruptedException {
        session.close();
    }
}
