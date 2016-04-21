package com.shuffle.sim;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.chan.Chan;
import com.shuffle.chan.Receive;
import com.shuffle.chan.Send;
import com.shuffle.player.Messages;
import com.shuffle.player.Packet;

import java.util.concurrent.TimeUnit;

/**
 * A network connecting different players together in a simulation.
 * (All it does is pretend to sign messages.)
 *
 * Created by Daniel Krawisz on 2/8/16.
 */
public class NetworkSim implements Send<Packet>, Receive<Messages.SignedPacket> {
    final Chan<Messages.SignedPacket> inbox;
    final SigningKey key;

    public NetworkSim(SigningKey key, Chan<Messages.SignedPacket> inbox) {

        this.key = key;
        this.inbox = inbox;
    }

    @Override
    public Messages.SignedPacket receive() throws InterruptedException {
        return inbox.receive();
    }

    @Override
    public Messages.SignedPacket receive(long l, TimeUnit u) throws InterruptedException {
        return inbox.receive(l, u);
    }

    @Override
    public boolean closed() {
        return inbox.closed();
    }

    @Override
    public boolean send(Packet packet) throws InterruptedException {
        return inbox.send(new Messages.SignedPacket(packet, key.makeSignature(packet)));
    }

    @Override
    public void close() throws InterruptedException {
        inbox.close();
    }
}
