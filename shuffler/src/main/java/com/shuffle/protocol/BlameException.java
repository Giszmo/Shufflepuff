package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/4/15.
 *
 * This exception is thrown when malicious behavior is detected on the part of another player.
 */
final class BlameException extends Exception {
    ShufflePhase phase;

    BlameException(ShufflePhase phase) {
        this.phase = phase;
    }
}
