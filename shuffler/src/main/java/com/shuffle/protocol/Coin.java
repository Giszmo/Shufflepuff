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
    void send(CoinTransaction t) throws CoinNetworkException;
    boolean unspent(CoinAddress addr);
    CoinAmount valueHeld(CoinAddress addr) throws BlockchainException, MempoolException;

    // Represents an amount of Bitcoin or other cryptocurrency.
    interface CoinAmount {
        boolean greater(CoinAmount ν) throws InvalidImplementationException;
    }

    interface CoinAddress {}

    // A representation of a Bitcoin or other cryptocurrency transaction.
    interface CoinTransaction {}
}
