package com.shuffle.cryptocoin;

/**
 * A representation of a Bitcoin or other cryptocurrency transaction.
 *
 * Created by Daniel Krawisz on 12/26/15.
 */
public interface Transaction {
    // Send the transaction into the network.
    void send() throws CoinNetworkError;
}
