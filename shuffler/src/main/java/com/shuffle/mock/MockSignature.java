package com.shuffle.mock;

import com.shuffle.bitcoin.Signature;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.protocol.Packet;

import java.io.Serializable;

/**
 * Created by Daniel Krawisz on 12/9/15.
 */
public class MockSignature implements Signature, Serializable {
    public final Transaction t;
    public final Packet packet;
    public final MockVerificationKey key;

    public MockSignature(Transaction t, MockVerificationKey key) {
        this.t = t;
        this.key = key;
        this.packet = null;
    }

    public MockSignature(Packet packet, MockVerificationKey key) {
        this.packet = packet;
        this.key = key;
        this.t = null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof MockSignature)) {
            return false;
        }

        MockSignature sig = (MockSignature)o;

        return (t == sig.t || t != null && t.equals(sig.t)) &&
                (packet == sig.packet || packet != null && packet.equals(sig.packet)) &&
                (key == sig.key || key != null && key.equals(sig.key));
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        if (packet != null) {
            return "signature[" + packet.toString() + ", " + key.toString() + "]";
        }
        if (t != null) {
            return "signature[" + t.toString() + ", " + key.toString() + "]";
        }
        return "signature[" + key.toString() + "]";
    }
}
