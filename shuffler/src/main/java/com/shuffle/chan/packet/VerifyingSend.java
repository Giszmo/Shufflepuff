package com.shuffle.chan.packet;

import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.Send;
import com.shuffle.p2p.Bytestring;

/**
 * Checks whether a message sent along the channel has been signed correctly by
 * a given key. The message is ignored if it is not.
 *
 * Created by Daniel Krawisz on 4/13/16.
 */
public class VerifyingSend<X> implements Send<Signed<X>> {

    private final Marshaller<X> marshaller;
    private final Send<Signed<X>> send;
    private final VerificationKey key;

    public VerifyingSend(
            Send<Signed<X>> send,
            Marshaller<X> marshaller,
            VerificationKey key) {

        if (marshaller == null || send == null || key == null) throw new NullPointerException();

        this.marshaller = marshaller;
        this.send = send;
        this.key = key;
    }

    @Override
    public boolean send(Signed<X> x) throws InterruptedException {
        return x != null && key.verify(marshaller.marshall(x.message), x.signature) && send.send(x);

    }

    @Override
    public void close() throws InterruptedException {
        send.close();
    }
}
