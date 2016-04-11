/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;

/**
 * Method to call when a peer receives a message.
 *
 * Created by Daniel Krawisz on 1/30/16.
 */
public interface Receiver<Message> {
    void receive(Message message) throws InterruptedException;
}
