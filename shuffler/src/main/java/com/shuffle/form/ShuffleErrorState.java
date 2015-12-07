package com.shuffle.form;

import com.shuffle.form.SessionIdentifier;
import com.shuffle.form.ShufflePhase;

/**
 * An error state that the protocol returns if the it fails.
 *
 * Created by Daniel Krawisz on 12/6/15.
 */
public class ShuffleErrorState {
    SessionIdentifier τ;
    ShufflePhase phase;
    Exception exception;

    public ShuffleErrorState(SessionIdentifier τ, ShufflePhase phase, Exception exception) {
        this.τ = τ;
        this.phase = phase;
        this.exception = exception;
    }
}
