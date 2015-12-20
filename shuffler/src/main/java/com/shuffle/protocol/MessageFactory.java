package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/6/15.
 */
public interface MessageFactory {
    Message make(); // Make a new packet.
    Message copy(Message packet) throws InvalidImplementationException;
}
