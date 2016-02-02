package com.shuffle.protocol;

import com.shuffle.bitcoin.VerificationKey;

import java.io.IOException;

/**
 * A network to the network of shuffle participants.
 *
 * Created by Daniel Krawisz on 12/7/15.
 */
public interface Network {
    void sendTo(VerificationKey to, SignedPacket packet) throws InvalidImplementationError, TimeoutError;
    SignedPacket receive() throws TimeoutError, InvalidImplementationError, InterruptedException, FormatException;
}
