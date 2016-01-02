package com.shuffle.protocol;

import com.shuffle.cryptocoin.VerificationKey;

/**
 * Created by Daniel Krawisz on 12/4/15.
 *
 * This exception is thrown when malicious behavior is detected on the part of another player.
 */
final class BlameException extends Exception {
    VerificationKey sender;
    Packet packet;

    // A blame message sent from another party.
    BlameException(VerificationKey sender, Packet packet) {
        this.sender = sender;
        this.packet = packet;
    }
}
