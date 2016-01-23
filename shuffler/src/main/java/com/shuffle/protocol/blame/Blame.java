package com.shuffle.protocol.blame;

import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.Signature;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.Packet;
import com.shuffle.protocol.SignedPacket;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Represents a message in which a player indicates that something has gone wrong, and which
 * possibly implicates another player as the culprit.
 *
 * Created by Daniel Krawisz on 1/22/16.
 */
public class Blame {
    final public Reason reason;
    final public VerificationKey accused; // Can be null if we don't know who to accuse yet.
    final public Transaction t;
    final public List<SignedPacket> packets;
    final public DecryptionKey privateKey;
    final public Map<VerificationKey, Signature> invalid;

    Blame(Reason reason,
          VerificationKey accused,
          Transaction t,
          DecryptionKey privateKey,
          List<SignedPacket> packets,
          Map<VerificationKey, Signature> invalid) {

        if (reason == null) {
            throw new IllegalArgumentException();
        }

        switch (reason) {
            case InsufficientFunds:
            case DoubleSpend: {
                if (accused == null || t == null) {
                    throw new IllegalArgumentException();
                }
                break;
            }
            case EquivocationFailure: {
                if (packets == null) {
                    throw new IllegalArgumentException();
                }
                break;
            }
            case ShuffleFailure:
            case NoFundsAtAll:
            case MissingOutput: {
                if (accused == null) {
                    throw new IllegalArgumentException();
                }
                break;
            }
            case ShuffleAndEquivocationFailure: {
                if (privateKey == null || packets == null) {
                    throw new IllegalArgumentException();
                }
                break;
            }
            case InvalidSignature: {
                if (invalid == null) {
                    throw new IllegalArgumentException();
                }
                break;
            }
        }

        this.reason = reason;
        this.accused = accused;
        this.t = t;
        this.packets = packets;
        this.invalid = invalid;
        this.privateKey = privateKey;
    }

    @Override
    public String toString() {
        String str = "Blame[";
        if (accused != null) {
            str += (accused.toString() + ", ");
        }
        if (packets != null) {
            str += (packets.toString() + ", ");
        }
        return str + reason.toString() + "]";
    }

    public Blame copy() {
        List<SignedPacket> packets = null;

        if (this.packets != null) {
            packets = new LinkedList<>();

            for (SignedPacket packet : this.packets) {

                packets.add(packet.copy());
            }
        }

        return new Blame(reason, accused, t, privateKey, packets, invalid);
    }

    // Sent when a player has insufficient funds in his address.
    public static Blame InsufficientFunds(VerificationKey accused, Transaction t) {
        return new Blame(Reason.InsufficientFunds, accused, t, null, null, null);
    }

    // Sent when a player has no funds in the address he provided.
    public static Blame NoFundsAtAll(VerificationKey accused) {
        return new Blame(Reason.NoFundsAtAll, accused, null, null, null, null);
    }

    // Sent when a player spends his coins while the protocol is in motion.
    public static Blame DoubleSpend(VerificationKey accused, Transaction t) {
        return new Blame(Reason.DoubleSpend, accused, t, null, null, null);
    }

    // Sent in phase 2 or three if an output is missing.
    public static Blame MissingOutput(VerificationKey accused) {
        return new Blame(Reason.MissingOutput, accused, null, null, null, null);
    }

    // Sent when a player makes an invalid signature to the transaction.
    public static Blame InvalidSignature(Map<VerificationKey, Signature> invalid) {
        return new Blame(Reason.InvalidSignature, null, null, null, null, invalid);
    }

    // Sent when something goes wrong in phase 4.
    public static Blame EquivocationFailure(List<SignedPacket> packets) {
        return new Blame(Reason.EquivocationFailure, null, null, null, packets, null);
    }

    // Sent when something goes wrong in phase 2.
    public static Blame ShuffleFailure() {
        return new Blame(Reason.ShuffleFailure, null, null, null, null, null);
    }

    // Sent when there is a failure in phase two and in the subsequent equivocation check.
    public static Blame ShuffleAndEquivocationFailure(DecryptionKey privateKey, List<SignedPacket> packets) {
        return new Blame(Reason.ShuffleFailure, null, null, privateKey, packets, null);
    }
}
