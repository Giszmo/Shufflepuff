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
import com.shuffle.bitcoin.CoinNetworkError;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;

import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Represents a fake Bitcoin network that can be manipulated for testing purposes.
 *
 * Created by Daniel Krawisz on 2/8/16.
 */
public interface MockCoin extends Coin {
    Coin mutated(); // Returns a new mock coin that produces mutated transactions.

    void put(Address addr, long value);

    Transaction spend(Address from, Address to, long amount);

    MockCoin copy();
}
