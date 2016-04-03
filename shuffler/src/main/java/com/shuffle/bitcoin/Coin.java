/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.bitcoin;

 import com.shuffle.bitcoin.Address;
 import com.shuffle.bitcoin.CoinNetworkException;
 import com.shuffle.bitcoin.Transaction;
 import com.shuffle.bitcoin.VerificationKey;

 import java.util.List;
 import java.util.Map;
 import java.util.Queue;

/**
 *
 * This interface provides service to the Bitcoin (or other) network. This includes queries to the
 * blockchain as well as to the p2p network. If these services cannot be provided while the protocol
 * is running, then the protocol must not be run.
 *
 * Created by Daniel Krawisz on 12/5/15.
 *
 */
public interface Coin {
    Transaction shuffleTransaction(
            long amount,
            List<VerificationKey> from,
            Queue<Address> to,
            Map<VerificationKey, Address> changeAddresses) throws CoinNetworkException;

    long valueHeld(Address addr) throws CoinNetworkException;

    // Returns either a transaction that sent from the given address that caused it to have .
    // insufficient funds or a transaction that sent to a given address that caused it to have
    // insufficient funds.
    Transaction getConflictingTransaction(Address addr, long amount);

    // Whether the given transaction spends the funds in the given address.
    Transaction getSpendingTransaction(Address addr, long amount);
}
