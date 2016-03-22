/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

import com.shuffle.bitcoin.Signature;
import com.shuffle.bitcoin.VerificationKey;

import java.io.Serializable;

/**
 * Represents a coin shuffle message with context information attached that is necessary to
 * detect low-level errors. The main protocol does not need to worry about that stuff, so
 * it just uses the Message type for most of what it does.
 *
 * Created by Daniel Krawisz on 12/9/15.
 */
public class Packet implements Serializable {

    final public Message message;
    final public SessionIdentifier session;
    final public Phase phase;
    final public VerificationKey signer;
    final public VerificationKey recipient;

    public Packet(Message message, SessionIdentifier session, Phase phase, VerificationKey signer, VerificationKey recipient) {
        if (session == null || phase == null || signer == null || recipient == null) {
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
        if (o == null) {
            return false;
        }

        if (!(o instanceof Packet)) {
            return false;
        }

        Packet packet = ((Packet)o);

        return session.equals(packet.session) && phase == packet.phase
                && signer.equals(packet.signer) && recipient.equals(packet.recipient)
                && message.equals(packet.message);
    }

    @Override
    public int hashCode() {
        int hash = message == null ? 0 : message.hashCode();
        hash = hash * 15 + session.hashCode();
        hash = hash * 15 + signer.hashCode();
        hash = hash * 15 + recipient.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "{" + message.toString() + ", " + session.toString() + ", " + phase.toString() + ", " + recipient.toString() + ", " + signer.toString() + "}";
    }
}
