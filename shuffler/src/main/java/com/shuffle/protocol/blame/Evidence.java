package com.shuffle.protocol.blame;

import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.bitcoin.Signature;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.Packet;
import com.shuffle.protocol.SignedPacket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Created by Daniel Krawisz on 1/22/16.
 *
 * Evidence collects the proof that a given player misbehaved during a run of the protocol.
 * This format is not sent over the internet and is for the user's records only.
 */
public class Evidence {
    private static Logger log = LogManager.getLogger(Evidence.class);

    public final VerificationKey accused;
    public final Reason reason;
    public final Transaction t;
    public final Signature signature;
    public final Map<VerificationKey, SignedPacket> output;
    public final Map<VerificationKey, EncryptionKey> sent;
    public final Map<VerificationKey, SignedPacket> shuffle;
    public final Map<VerificationKey, SignedPacket> broadcast;
    public final Map<VerificationKey, DecryptionKey> keys;

    protected Evidence(
            VerificationKey accused,
            Reason reason,
            Transaction t,
            Signature signature,
            Map<VerificationKey, SignedPacket> output,
            Map<VerificationKey, EncryptionKey> sent,
            Map<VerificationKey, SignedPacket> shuffle,
            Map<VerificationKey, SignedPacket> broadcast,
            Map<VerificationKey, DecryptionKey> keys) {

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
                if (shuffle == null || broadcast == null || keys == null) {
                    throw new IllegalArgumentException();
                }
                break;
            }
            case InvalidSignature: {
                if (signature == null) {
                    throw new IllegalArgumentException();
                }
                break;
            }
            case NoFundsAtAll:
                break;
            default: {
                throw new IllegalArgumentException();
            }
        }

        this.accused = accused;
        this.reason = reason;
        this.t = t;
        this.signature = signature;
        this.output = output;
        this.sent = sent;
        this.keys = keys;
        this.shuffle = shuffle;
        this.broadcast = broadcast;
    }

    protected Evidence(VerificationKey accused, Reason reason, boolean credible) {
        this.accused = accused;
        this.reason = reason;
        this.t = null;
        this.signature = null;
        this.output = null;
        this.sent = null;
        this.keys = null;
        this.broadcast = null;
        this.shuffle = null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof Evidence)) {
            return false;
        }

        Evidence b = (Evidence) o;

        return reason == b.reason &&
                (t == null && b.t == null || t != null && b.t != null && t.equals(b.t));
    }

    @Override
    public int hashCode() {
        int z = 17;
        int hash = 0;

        hash = hash * z + (reason == null ? 0 : reason.hashCode());
        hash = hash * z + (accused == null ? 0 : accused.hashCode());
        hash = hash * z + (t == null ? 0 : t.hashCode());
        hash = hash * z + (signature == null ? 0 : signature.hashCode());
        hash = hash * z + (output == null ? 0 : output.hashCode());
        hash = hash * z + (sent == null ? 0 : sent.hashCode());
        hash = hash * z + (shuffle == null ? 0 : shuffle.hashCode());
        hash = hash * z + (broadcast == null ? 0 : broadcast.hashCode());
        hash = hash * z + (keys == null ? 0 : keys.hashCode());

        return hash;
    }

    public boolean match(Evidence e) {
        return e != null && reason == e.reason;
    }

    @Override
    public String toString() {
        String str = reason.toString();
        if (t != null) {
            str += ":" + t.toString();
        }
        return str;
    }

    static public Evidence NoFundsAtAll(VerificationKey accused) {
        return new Evidence(accused, Reason.NoFundsAtAll, null, null, null, null, null, null, null);
    }

    static public Evidence InsufficientFunds(VerificationKey accused, Transaction t) {
        return new Evidence(accused, Reason.InsufficientFunds, t, null, null, null, null, null, null);
    }

    static public Evidence DoubleSpend(VerificationKey accused, Transaction t) {
        return new Evidence(accused, Reason.DoubleSpend, t, null, null, null, null, null, null);
    }

    static public Evidence InvalidSignature(VerificationKey accused, Signature signature) {
        return new Evidence(accused, Reason.InvalidSignature, null, signature, null, null, null, null, null);
    }

    static public Evidence EquivocationFailureAnnouncement(VerificationKey accused, Map<VerificationKey, EncryptionKey> sent) {
        return new Evidence(accused, Reason.EquivocationFailure, null, null, null, sent, null, null, null);
    }

    static public Evidence EquivocationFailureBroadcast(VerificationKey accused, Map<VerificationKey, SignedPacket> output) {
        return new Evidence(accused, Reason.EquivocationFailure, null, null, output, null, null, null, null);
    }

    static public Evidence ShuffleMisbehaviorDropAddress(VerificationKey accused,
                                                         Map<VerificationKey, DecryptionKey> keys,
                                                         Map<VerificationKey, SignedPacket> shuffleMessages,
                                                         Map<VerificationKey, SignedPacket> broadcastMessages) {
        return new Evidence(accused, Reason.ShuffleFailure, null, null, null, null, shuffleMessages, broadcastMessages, keys);
    }

    static public Evidence Expected(VerificationKey accused, Reason reason, boolean credible) {
        return new Evidence(accused, reason, credible);
    }

    // TODO remove this function when the protocol is finally done.
    static public Evidence Placeholder(VerificationKey accused, Reason reason, boolean credible) {
        log.warn("placeholder evidence!");
        new Exception().printStackTrace();
        return new Evidence(accused, reason, credible);
    }
}
