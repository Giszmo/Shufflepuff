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
public class VerifyingSend<X> implements Send<Bytestring> {

    private final Marshaller<Signed<X>> marshaller;
    private final Send<Signed<X>> send;

    public VerifyingSend(
            Send<Signed<X>> send,
            Marshaller<X> marshaller,
            VerificationKey key) {

        if (marshaller == null || send == null || key == null) throw new NullPointerException();

        this.marshaller = new VerifyingMarshaller<>(key, marshaller);
        this.send = send;
    }

    @Override
    public boolean send(Bytestring bytestring) throws InterruptedException {

        Signed<X> x = marshaller.unmarshall(bytestring);

        return x != null && send.send(x);

    }

    @Override
    public void close() throws InterruptedException {
        send.close();
    }
}
