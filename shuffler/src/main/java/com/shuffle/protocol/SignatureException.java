package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 1/27/16.
 */
public class SignatureException extends Exception {
    SignedPacket packet;

    public SignatureException(SignedPacket packet) {
        if (packet == null) {
            throw new NullPointerException();
        }
        this.packet = packet;
    }
}
