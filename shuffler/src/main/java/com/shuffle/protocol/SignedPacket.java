package com.shuffle.protocol;

import com.shuffle.bitcoin.Signature;

/**
 * Represents a packet that has been digitally signed.
 *
 * Created by Daniel Krawisz on 1/22/16.
 */
public class SignedPacket {
    public final Packet payload;
    public final Signature signature;

    public SignedPacket(Packet payload, Signature signature) {
        if (payload == null || signature == null) {
            throw new NullPointerException();
        }
        this.payload = payload;
        this.signature = signature;
    }

    public boolean verify() {
        return payload.signer.verify(payload, signature);
    }

    public SignedPacket copy() {
        return new SignedPacket(payload.copy(), signature.copy());
    }

    @Override
    public String toString() {
        return payload.toString() + "[" + signature.toString() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof SignedPacket)) {
            return false;
        }

        SignedPacket sp = (SignedPacket)o;

        return payload.equals(sp.payload) && signature.equals(sp.signature);
    }

    @Override
    public int hashCode() {
        return payload.hashCode() + signature.hashCode();
    }
}
