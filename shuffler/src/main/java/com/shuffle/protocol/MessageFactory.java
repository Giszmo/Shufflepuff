package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/6/15.
 */
public interface MessageFactory {
    Message make(SessionIdentifier τ, ShufflePhase phase, SigningKey sk); // Make a new packet.
    Message copy(Message message) throws InvalidImplementationException;
}
