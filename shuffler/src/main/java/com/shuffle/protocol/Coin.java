package com.shuffle.protocol;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * TO BE IMPLEMENTED BY THE USER
 *
 * This interface provides service to the Bitcoin (or other) network. This includes queries to the block
 * chain as well as to the p2p network. If these services cannot be provided while the protocol
 * is running, then the protocol must not be run.
 *
 * Created by Daniel Krawisz on 12/5/15.
 *
 */
public interface Coin {
    CoinTransaction transaction(List<CoinAddress> inputs, LinkedHashMap<CoinAddress, CoinAmount> outputs);
    void send(CoinTransaction t) throws CoinNetworkError;

    CoinAmount valueHeld(CoinAddress addr) throws BlockchainError, MempoolError;

    // Returns either a transaction that sent from the given address that caused it to have .
    // insufficient funds or a transaction that sent to a given address that caused it to have
    // insufficient funds.
    CoinTransaction getOffendingTransaction(CoinAddress addr, CoinAmount ν);

    // Returns the transaction that sent to a given address.

    // Represents an amount of Bitcoin or other cryptocurrency.
    interface CoinAmount {
        boolean greater(CoinAmount ν) throws InvalidImplementationError;
    }

    interface CoinAddress {}

    // A representation of a Bitcoin or other cryptocurrency transaction.
    interface CoinTransaction {}

    // Representing a digital signature of a bitcoin transaction.
    interface CoinSignature {
    }
}
