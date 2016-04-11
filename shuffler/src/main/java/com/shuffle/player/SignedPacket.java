/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.player;

import com.shuffle.bitcoin.Signature;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.message.Packet;
import com.shuffle.protocol.message.Phase;

import java.io.Serializable;

/**
 * Represents a packet that has been digitally signed.
 *
 * Created by Daniel Krawisz on 1/22/16.
 */
public class SignedPacket implements Packet, Serializable{

    private class Packet implements com.shuffle.protocol.message.Packet, Serializable {

        public final Message message;
        public final Phase phase;
        public final VerificationKey to;

        Packet(
                Message message,
                Phase phase,
                VerificationKey to
        ) {

            this.to = to;
            this.phase = phase;
            this.message = message;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }

            if (!(o instanceof Packet)) {
                return false;
            }

            Packet packet = ((Packet)o);

            return phase == packet.phase && to.equals(packet.to)
                    && message.from.equals(packet.message.from)
                    && message.equals(packet.message);
        }

        @Override
        public int hashCode() {
            int hash = message.hashCode();
            hash = hash * 15 + to.hashCode();
            hash = hash * 15 + message.from.hashCode();
            return hash;
        }

        @Override
        public String toString() {
            return "{" + message.toString() + ", " + phase.toString() + ", "
                    + to.toString() + ", " + message.from + "}";
        }

        @Override
        public Message payload() {
            return message;
        }

        @Override
        public Phase phase() {
            return phase;
        }

        @Override
        public VerificationKey from() {
            return message.from;
        }

        @Override
        public VerificationKey to() {
            return to;
        }
    }

    public final Packet payload;
    public final Signature signature;

    public SignedPacket(Packet payload, Signature signature) {
        if (payload == null || signature == null) {
            throw new NullPointerException();
        }
        this.payload = payload;
        this.signature = signature;
    }

    public SignedPacket(
            Message message,
            Phase phase,
            VerificationKey to,
            SigningKey from) {

        if (message == null || phase == null || to == null || from == null) {
            throw new NullPointerException();
        }

        payload = new Packet(message, phase, to);

        signature = from.makeSignature(payload);
    }

    public boolean verify() {
        return payload.message.from.verify(this, signature);
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


    @Override
    public Message payload() {
        return payload.message;
    }

    @Override
    public Phase phase() {
        return payload.phase();
    }

    @Override
    public VerificationKey from() {
        return payload.from();
    }

    @Override
    public VerificationKey to() {
        return payload.to();
    }
}
