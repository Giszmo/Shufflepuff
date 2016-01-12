package com.shuffle.protocol;

/**
 * An error state that the protocol returns if the it fails.
 *
 * Created by Daniel Krawisz on 12/6/15.
 */
public class ReturnState {
    boolean success;
    SessionIdentifier session;
    Phase phase;
    Throwable error = null;
    BlameMatrix blame = null;

    public ReturnState(boolean success, SessionIdentifier session, Phase phase, Throwable error, BlameMatrix blame) {
        this.success = success;
        this.session = session;
        this.phase = phase;
        this.error = error;
        this.blame = blame;
    }

    // Whether two return states are equivalent.
    public boolean match(ReturnState m) {
        return success == m.success && session == m.session && phase == m.phase &&
                ((error == null && m.error == null) ||
                (error != null && m.error != null && error.getClass().equals(m.error.getClass())))
                && ((blame == null && m.blame == null) || (blame != null && m.blame != null && blame.equals(m.blame)));
    }

    public String toString() {
        String session;
        if (this.session != null) {
            session = " " + this.session.toString();
        } else {
            session = "";
        }

        if (success) {
            return "Successful run" + session;
        }

        if (error != null) {
            return "Unsuccessful run" + session + "; threw " + error.toString() + " in phase " + phase.toString();
        }

        if (blame != null) {
            return "Unsuccessful run" + session + " failed in phase " + phase.toString() + "; blame = " + blame.toString();
        }

        return "Unsuccessful run" + session + " failed in phase " + phase.toString();
    }
}
