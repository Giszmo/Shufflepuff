package com.shuffle.bitcoin;

/**
 * A representation of a BitcoinCrypto or other cryptocurrency transaction.
 *
 * Created by Daniel Krawisz on 12/26/15.
 */
public interface Transaction {
    // Send the transaction into the network.
    void send() throws CoinNetworkError;
}
