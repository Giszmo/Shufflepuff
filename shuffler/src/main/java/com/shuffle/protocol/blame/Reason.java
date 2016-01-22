package com.shuffle.protocol.blame;

/**
 * Created by Daniel Krawisz on 1/22/16.
 */
public enum Reason {
    InsufficientFunds,
    NoFundsAtAll,
    DoubleSpend,
    EquivocationFailure,
    ShuffleFailure,
    ShuffleAndEquivocationFailure,
    InvalidSignature,
    MissingOutput,
}
