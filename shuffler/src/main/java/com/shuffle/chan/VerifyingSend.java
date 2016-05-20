package com.shuffle.chan;

import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.p2p.Bytestring;
import com.shuffle.player.Messages;

import java.util.concurrent.TimeUnit;

/**
 * Ensures that all messages are signed and properly formatted after they are received.
 *
 * Created by Daniel Krawisz on 4/13/16.
 */
public class VerifyingSend<X> implements Send<Bytestring> {

    public static class Signed<X> {
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

    private final SigningSend.Marshaller<X> marshaller;
    private final Send<Signed<X>> send;
    private final VerificationKey key;

    public VerifyingSend(
            Send<Signed<X>> send,
            SigningSend.Marshaller<X> marshaller,
            VerificationKey key) {

        if (marshaller == null || send == null || key == null) throw new NullPointerException();

        this.marshaller = marshaller;
        this.send = send;
        this.key = key;
    }

    @Override
    public boolean send(Bytestring bytestring) throws InterruptedException {

        if (bytestring == null) return false;

        Bytestring[] stripped = key.verify(bytestring);

        if (stripped == null) return false;

        X x = marshaller.unmarshall(stripped[0]);

        return x != null && send.send(new Signed<>(x, stripped[1]));

    }

    @Override
    public void close() throws InterruptedException {
        send.close();
    }
}
