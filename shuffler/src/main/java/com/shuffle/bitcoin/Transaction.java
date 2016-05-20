/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.bitcoin;

import com.shuffle.p2p.Bytestring;

import java.io.Serializable;

/**
 * A representation of a Bitcoin or other cryptocurrency transaction.
 *
 * Created by Daniel Krawisz on 12/26/15.
 */
public interface Transaction extends Serializable {
    // Send the transaction into the network.
    boolean send() throws CoinNetworkException;

    Bytestring serialize();
}
