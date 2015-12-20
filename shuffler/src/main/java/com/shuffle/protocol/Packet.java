package com.shuffle.protocol;

import java.util.Iterator;
import java.util.Queue;

/**
 *
 *
 * Created by Daniel Krawisz on 12/9/15.
 */
public class Packet {

    Message message;
    SessionIdentifier τ;
    ShufflePhase phase;
    VerificationKey signer;

    public Packet(Message message, SessionIdentifier τ, ShufflePhase phase, VerificationKey signer) {
        if (τ == null || phase == null || signer == null) {
            throw new NullPointerException();
        }

        this.signer = signer;
        this.phase = phase;
        this.τ = τ;
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Packet)) {
            return false;
        }

        Packet packet = ((Packet)o);

        return τ.equals(packet.τ) && phase == packet.phase && signer.equals(packet.signer) && message.equals(packet.message);
    }

    @Override
    public String toString() {
        return "{" + message.toString() + ", " + τ.toString() + ", " + phase.toString() + ", " + signer.toString() + "}";
    }
}
