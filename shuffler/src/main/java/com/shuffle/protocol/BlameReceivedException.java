package com.shuffle.protocol;


/**
 * To be thrown when a blame message is received from another party.
 *
 * Created by Daniel Krawisz on 12/7/15.
 */
final class BlameReceivedException extends Exception {
    VerificationKey sender;
    Message message;

    // A blame message sent from another party.
    BlameReceivedException(VerificationKey sender, Message message) {
        this.sender = sender;
        this.message = message;
    }
}
