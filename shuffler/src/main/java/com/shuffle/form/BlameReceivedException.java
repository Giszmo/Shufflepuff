package com.shuffle.form;

/**
 * To be thrown when a blame message is received from another party.
 *
 * Created by Daniel Krawisz on 12/7/15.
 */
public class BlameReceivedException extends Exception {
    VerificationKey sender;
    Packet packet;

    // A blame message sent from another party.
    BlameReceivedException(VerificationKey sender, Packet packet) {
        this.sender = sender;
        this.packet = packet;
    }
}
