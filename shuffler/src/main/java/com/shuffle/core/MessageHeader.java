package com.shuffle.core;

import com.shuffle.form.SessionIdentifier;
import com.shuffle.form.ShufflePhase;
import com.shuffle.form.CoinSignature;
import com.shuffle.form.VerificationKey;

/**
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MessageHeader {
    SessionIdentifier τ;
    VerificationKey from;
    ShufflePhase phase;

    public MessageHeader(SessionIdentifier τ, VerificationKey from, ShufflePhase phase) {
        this.τ = τ;
        this.from = from;
        this.phase = phase;
    }
}
