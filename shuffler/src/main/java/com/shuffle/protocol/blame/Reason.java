/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol.blame;

/**
 * Reasons for entering the blame phase.
 *
 * Created by Daniel Krawisz on 1/22/16.
 */
public enum Reason {
    InsufficientFunds, // A player does not have enough funds.
    NoFundsAtAll, // No funds at all in a player's address.
    DoubleSpend, // A player spends his funds while the protocol is in progress.
    EquivocationFailure, // Something goes wrong during the equivocation phase.
    ShuffleFailure, // Something goes wrong during the shuffle phase.
    ShuffleAndEquivocationFailure, // There is an error case in which something
                                    // Goes wrong during the shuffle phase and then we skip
                                    // straight to the equivocation phase. If that fails, then
                                    // we send this message.
    InvalidSignature, // A signature has come out invalid.
    MissingOutput, // An output is missing from the output vector.
    Liar, // A player has falsely accused another of being malicious.
}
