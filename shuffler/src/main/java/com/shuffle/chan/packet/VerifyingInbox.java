package com.shuffle.chan.packet;

import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.BasicInbox;
import com.shuffle.chan.Inbox;
import com.shuffle.chan.Send;
import com.shuffle.p2p.Bytestring;

import java.util.concurrent.TimeUnit;

/**
 * An inbox that verifyies signatures at every step.
 *
 * Created by Daniel Krawisz on 5/27/16.
 */
public class VerifyingInbox<X> implements Inbox<VerificationKey, Signed<X>, Bytestring> {
    private final Inbox<VerificationKey, Signed<X>, Signed<X>> inner;
    private final Marshaller<X> marshaller;

    public VerifyingInbox(Inbox<VerificationKey, Signed<X>, Signed<X>> inner, Marshaller<X> marshaller) {
        this.inner = inner;
        this.marshaller = marshaller;
    }

    public VerifyingInbox(int cap, Marshaller<X> marshaller) {
        this.marshaller = marshaller;
        this.inner = new BasicInbox<>(cap);
    }

    @Override
    public Send<Bytestring> receivesFrom(VerificationKey from) {
        Send<Signed<X>> onward = inner.receivesFrom(from);

        if (onward == null) return null;

        return new VerifyingSend<>(onward, marshaller, from);
    }

    @Override
    public void close() throws InterruptedException {
        inner.close();
    }

    @Override
    public Envelope<VerificationKey, Signed<X>> receive() throws InterruptedException {
        return inner.receive();
    }

    @Override
    public Envelope<VerificationKey, Signed<X>> receive(long l, TimeUnit u) throws InterruptedException {
        return inner.receive(l, u);
    }

    @Override
    public boolean closed() {
        return inner.closed();
    }
}
