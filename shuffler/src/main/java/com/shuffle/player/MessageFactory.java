/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.player;

import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.Network;

/**
 * Created by Daniel Krawisz on 12/9/15.
 */
public class MessageFactory implements com.shuffle.protocol.message.MessageFactory {
    private final Network net;
    private final SessionIdentifier session;
    private final VerificationKey me;

    public MessageFactory(SessionIdentifier session, VerificationKey me, Network net) {
        this.net = net;
        this.session = session;
        this.me = me;
    }

    @Override
    public com.shuffle.protocol.message.Message make() {
        return new Message(session, me, net);
    }
}
