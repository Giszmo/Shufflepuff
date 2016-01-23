package com.shuffle.protocol;

import com.shuffle.bitcoin.Signature;

/**
 * Represents a packet that has been digitally signed.
 *
 * Created by Daniel Krawisz on 1/22/16.
 */
public class SignedPacket {
    public final Packet packet;
    public final Signature signature;

    public SignedPacket(Packet packet, Signature signature) {
        if (packet == null || signature == null) {
            throw new NullPointerException();
        }
        this.packet = packet;
        this.signature = signature;
    }

    public boolean verify() {
        return packet.signer.verify(packet, signature);
    }

    public SignedPacket copy() {
        return new SignedPacket(packet.copy(), signature);
    }
}
