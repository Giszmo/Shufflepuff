package com.shuffle.protocol;

import java.util.Set;

/**
 * An error state that the protocol returns if the it fails.
 *
 * Created by Daniel Krawisz on 12/6/15.
 */
public final class ReturnState {
    boolean success;
    SessionIdentifier τ;
    ShufflePhase phase;
    Exception exception = null;
    Set<ShuffleMachine.Blame> blame = null;

    public ReturnState(boolean success, SessionIdentifier τ, ShufflePhase phase, Throwable exception, Set<ShuffleMachine.Blame> blame) {
        this.success = success;
        this.τ = τ;
        this.phase = phase;
        this.exception = exception;
        this.blame = blame;
    }

    // Whether two return states are equivalent.
    public boolean match(ReturnState m) {
        return success == m.success && τ == m.τ && phase == m.phase &&
                ((exception == null && m.exception == null) ||
                (exception != null && m.exception != null && exception.getClass().equals(m.exception.getClass())))
                && ((blame == null && m.blame == null) || (blame != null && m.blame != null && blame.equals(m.blame)));
    }

    public String toString() {
        String session;
        if (τ != null) {
            session = " " + τ.toString();
        } else {
            session = "";
        }

        if (success) {
            return "Successful run" + session;
        }

        if (exception != null) {
            return "Unsuccessful run" + session + "; threw " + exception.toString() + " in phase " + phase.toString();
        }

        return "Unsuccessful run" + session + " failed in phase " + phase.toString();
    }
}
