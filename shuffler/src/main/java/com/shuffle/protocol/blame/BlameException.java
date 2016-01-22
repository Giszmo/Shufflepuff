package com.shuffle.protocol.blame;

import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.Packet;

/**
 * Created by Daniel Krawisz on 12/4/15.
 *
 * This exception is thrown when malicious behavior is detected on the part of another player.
 */
final public class BlameException extends Exception {
    final VerificationKey sender;
    final Packet packet;

    // A blame message history from another party.
    public BlameException(VerificationKey sender, Packet packet) {
        this.sender = sender;
        this.packet = packet;
    }
}
