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
 * To be called when we have been unable to receive messages from players from
 * whom we have been expecting them.
 *
 * Created by Daniel Krawisz on 12/4/15.
 */
public class WaitingException extends Exception {
    public final Queue<VerificationKey> waitingOn = new LinkedList<>();

    public WaitingException(Collection<VerificationKey> w) {
        waitingOn.addAll(w);
    }

    public WaitingException(VerificationKey v) {
        waitingOn.add(v);
    }

    @Override
    public String getMessage() {
        return "CoinShuffle Timeout Exception";
    }
}
