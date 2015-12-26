package com.shuffle.protocol;

import com.shuffle.cryptocoin.VerificationKey;

/**
 * A connection to the network of shuffle participants.
 *
 * Created by Daniel Krawisz on 12/7/15.
 */
public interface Network {
    void sendTo(VerificationKey to, Packet packet) throws InvalidImplementationError, TimeoutError;
    Packet receive() throws TimeoutError, InvalidImplementationError, InterruptedException;
}
