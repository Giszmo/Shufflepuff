/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

import com.shuffle.bitcoin.VerificationKey;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

/**
 * To be called when the network times out.
 *
 * Created by Daniel Krawisz on 12/4/15.
 */
public class TimeoutException extends Exception {
    public final Queue<VerificationKey> waitingOn = new LinkedList<>();

    public TimeoutException(Collection<VerificationKey> w) {
        waitingOn.addAll(w);
    }

    public TimeoutException(VerificationKey v) {
        waitingOn.add(v);
    }

    @Override
    public String getMessage() {
        return "CoinShuffle Timeout Exception";
    }
}
