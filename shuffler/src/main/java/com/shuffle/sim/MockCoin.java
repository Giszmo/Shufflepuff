package com.shuffle.sim;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Coin;
import com.shuffle.bitcoin.Transaction;

/**
 * Represents a fake Bitcoin network that can be manipulated for testing purposes.
 *
 * Created by Daniel Krawisz on 2/8/16.
 */
public interface MockCoin extends Coin {
    void put(Address addr, long value);

    Transaction spend(Address from, Address to, long amount);
}
