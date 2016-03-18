/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

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
    Coin mutated(); // Returns a new mock coin that produces mutated transactions.

    void put(Address addr, long value);

    // Make a transaction that spends the coins from a given address.
    Transaction makeSpendingTransaction(Address from, Address to, long amount);

    MockCoin copy();
}
