package com.shuffle.protocol.blame;

import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.bitcoin.Signature;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.Packet;

import java.util.Map;

/**
 * Created by Daniel Krawisz on 1/22/16.
 *
 * Evidence collects the proof that a given player misbehaved during a run of the protocol.
 * This format is not sent over the internet and is for the user's records only.
 */
public class Evidence {
    Reason reason;
    boolean credible; // Do we believe this evidence?
    Transaction t = null;
    Signature signature = null;
    Map<VerificationKey, Packet> output = null;
    Map<VerificationKey, EncryptionKey> sent = null;

    private Evidence(
            Reason reason,
            boolean credible,
            Transaction t,
            Signature signature,
            Map<VerificationKey, Packet> output,
            Map<VerificationKey, EncryptionKey> sent) {

        if (reason == null) {
            throw new IllegalArgumentException();
        }

        switch (reason) {
            case InsufficientFunds: {
                if (t == null) {
                    throw new IllegalArgumentException();
                }
                break;
            }
            case DoubleSpend: {
                if (t == null) {
                    throw new IllegalArgumentException();
                }
                break;
            }
            case EquivocationFailure: {
                if ((output == null && sent == null) || (output != null && sent != null)) {
                    throw new IllegalArgumentException();
                }
                break;
            }
            case ShuffleFailure: {
                // TODO
                throw new IllegalArgumentException();
            }
            case MissingOutput: {
                // TODO
                throw new IllegalArgumentException();
            }
            case InvalidSignature: {
                if (signature == null) {
                    throw new IllegalArgumentException();
                }
                break;
            }
        }

        this.reason = reason;
        this.credible = credible;
        this.t = t;
        this.signature = signature;
        this.output = output;
        this.sent = sent;
    }

    public Evidence(Reason reason, boolean credible) {
        // A transaction should always be included with InsufficientFunds.
        if (reason == Reason.InsufficientFunds) {
            throw new NullPointerException();
        }
        this.reason = reason;
        this.credible = true;
    }

    public Evidence(Reason reason, boolean credible, Transaction t) {
        if (t == null) {
            throw new NullPointerException();
        }
        this.reason = reason;
        this.credible = true;
        this.t = t;
    }

    protected Evidence() {

    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Evidence)) {
            return false;
        }

        Evidence b = (Evidence) o;

        return reason == b.reason && credible == b.credible &&
                (t == null && b.t == null || t != null && b.t != null && t.equals(b.t));
    }

    @Override
    public int hashCode() {
        int hash = credible ? 0 : 1;
        hash = hash * 2 + (output == null ? 0 : output.hashCode());
        hash = hash * 15 + (reason == null ? 0 : reason.hashCode());
        hash = hash * 15 + (signature == null ? 0 : signature.hashCode());
        return hash;
    }

    public boolean match(Evidence e) {
        return equals(e);
    }

    @Override
    public String toString() {
        String str = reason.toString();
        if (t != null) {
            str += ":" + t.toString();
        }
        return str;
    }

    static public Evidence NoFundsAtAll(boolean credible) {
        return new Evidence(Reason.NoFundsAtAll, credible, null, null, null, null);
    }

    static public Evidence InsufficientFunds(boolean credible, Transaction t) {
        return new Evidence(Reason.InsufficientFunds, credible, t, null, null, null);
    }

    static public Evidence DoubleSpend(boolean credible, Transaction t) {
        return new Evidence(Reason.DoubleSpend, credible, t, null, null, null);
    }

    static public Evidence InvalidSignature(boolean credible, Signature signature) {
        return new Evidence(Reason.InvalidSignature, credible, null, signature, null, null);
    }

    static public Evidence EquivocationFailureAnnouncement(Map<VerificationKey, EncryptionKey> sent) {
        return new Evidence(Reason.EquivocationFailure, true, null, null, null, sent);
    }

    static public Evidence EquivocationFailureBroadcast(Map<VerificationKey, Packet> output) {
        return new Evidence(Reason.EquivocationFailure, true, null, null, output, null);
    }
}
