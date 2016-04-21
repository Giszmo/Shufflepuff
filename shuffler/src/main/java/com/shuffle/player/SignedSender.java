package com.shuffle.player;

import com.shuffle.chan.Send;
import com.shuffle.p2p.Bytestring;
import com.shuffle.p2p.Session;

/**
 * Created by Daniel Krawisz on 4/13/16.
 */
public class SignedSender<Address> implements Send<Packet> {

    final Session<Address, Bytestring> session;
    final Marshaller marshaller;

    public SignedSender(Session<Address, Bytestring> session, Marshaller marshaller) {
        this.session = session;
        this.marshaller = marshaller;
    }

    @Override
    public boolean send(Packet packet) throws InterruptedException {
        Bytestring b = marshaller.marshallAndSign(packet);
        return b != null && session.send(b);

    }

    @Override
    public void close() throws InterruptedException {
        session.close();
    }
}
