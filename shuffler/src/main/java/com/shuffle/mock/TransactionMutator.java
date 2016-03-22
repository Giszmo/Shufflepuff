/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.mock;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Coin;
import com.shuffle.bitcoin.CoinNetworkError;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;

import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Created by Daniel Krawisz on 3/9/16.
 */
public class TransactionMutator implements Coin {
    final MockCoin coin;

    public TransactionMutator(MockCoin coin) {
        this.coin = coin;
    }


    @Override
    public Transaction shuffleTransaction(long amount, List<VerificationKey> from, Queue<Address> to, Map<VerificationKey, Address> changeAddresses) throws CoinNetworkError {
        MockCoin.MockTransaction tr = (MockCoin.MockTransaction) coin.shuffleTransaction(amount, from, to, changeAddresses);
        return coin.new MockTransaction(tr.inputs, tr.outputs, tr.z + 1);
    }

    @Override
    public long valueHeld(Address addr) throws CoinNetworkError {
        return coin.valueHeld(addr);
    }

    @Override
    public Transaction getConflictingTransaction(Address addr, long amount) {
        return coin.getConflictingTransaction(addr, amount);
    }

    @Override
    public Transaction getSpendingTransaction(Address addr, long amount) {
        return coin.getSpendingTransaction(addr, amount);
    }
}
