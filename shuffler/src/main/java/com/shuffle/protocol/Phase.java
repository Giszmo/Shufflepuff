/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

/**
 * The phases of the shuffle protocol.
 *
 * Created by Daniel Krawisz on 12/3/15.
 */
public enum Phase {
    Uninitiated,
    Announcement, // Everone generates new encryption keys and distributes them to one another.
    Shuffling, // In turn, each of the players adds his own new address and reshufles the result.
    BroadcastOutput, // The final output order is broadcast to everyone.
    EquivocationCheck, // Check that everyone has the same set of inputs.
    VerificationAndSubmission, // Generate transaction, distribute signatures, and send it off.
    Completed,
    Blame, // Someone has attempted to cheat.
    Error,
}
