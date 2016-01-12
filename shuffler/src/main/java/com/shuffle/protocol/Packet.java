package com.shuffle.protocol;

import com.shuffle.cryptocoin.VerificationKey;

/**
 * Represents a coin shuffle message with context information attached that is necessary to
 * detect low-level errors. The main protocol does not need to worry about that stuff, so
 * it just uses the Message type for most of what it does.
 *
 * Created by Daniel Krawisz on 12/9/15.
 */
public class Packet {

    Message message;
    SessionIdentifier session;
    Phase phase;
    VerificationKey signer;
    VerificationKey recipient;

    public Packet(Message message, SessionIdentifier session, Phase phase, VerificationKey signer, VerificationKey recipient) {
        if (session == null || phase == null || signer == null) {
            throw new NullPointerException();
        }

        this.signer = signer;
        this.recipient = recipient;
        this.phase = phase;
        this.session = session;
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Packet)) {
            return false;
        }

        Packet packet = ((Packet)o);

        return session.equals(packet.session) && phase == packet.phase && signer.equals(packet.signer) && message.equals(packet.message);
    }

    @Override
    public String toString() {
        return "{" + message.toString() + ", " + session.toString() + ", " + phase.toString() + ", " + signer.toString() + "}";
    }
}
