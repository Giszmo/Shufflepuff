package com.shuffle.form;

/**
 * The session identifier is a unique string given at the outset.
 *
 * Created by Daniel Krawisz on 12/3/15.
 */
public interface SessionIdentifier {
    boolean equals(SessionIdentifier τ) throws InvalidImplementationException;
}
