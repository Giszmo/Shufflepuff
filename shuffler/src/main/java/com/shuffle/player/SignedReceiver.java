package com.shuffle.player;

import com.shuffle.chan.Chan;
import com.shuffle.chan.Receive;
import com.shuffle.chan.Send;
import com.shuffle.p2p.Bytestring;

import java.util.concurrent.TimeUnit;

/**
 * Ensures that all messages are signed and properly formatted after they are received.
 *
 * Created by Daniel Krawisz on 4/13/16.
 */
public class SignedReceiver implements Receive<Messages.SignedPacket>, Send<Bytestring> {
    private final Marshaller marshaller;
    private final Chan<Bytestring> chan;

    public SignedReceiver(Marshaller marshaller, Chan<Bytestring> chan) {
        this.marshaller = marshaller;
        this.chan = chan;
    }

    @Override
    public Messages.SignedPacket receive() throws InterruptedException {
        while (true) {
            Messages.SignedPacket packet = marshaller.unmarshall(chan.receive());

            if (packet != null) return packet;
        }
    }

    @Override
    public Messages.SignedPacket receive(long l, TimeUnit u) throws InterruptedException {
        long started = System.currentTimeMillis();

        long total = u.convert(l, TimeUnit.MILLISECONDS);

        while (true) {

            long now = System.currentTimeMillis();

            long allowed = total + started - now;

            Bytestring str = chan.receive(allowed, TimeUnit.MILLISECONDS);

            if (str == null) continue;

            Messages.SignedPacket packet = marshaller.unmarshall(str);

            if (packet != null) return packet;
        }
    }

    @Override
    public boolean closed() {
        return false;
    }

    @Override
    public boolean send(Bytestring bytestring) throws InterruptedException {
        return chan.send(bytestring);
    }

    @Override
    public void close() throws InterruptedException {
        chan.close();
    }
}
