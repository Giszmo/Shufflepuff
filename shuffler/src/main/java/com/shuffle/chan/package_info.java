/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 * Package chan provides for channels that work like the chan type in golang, because those are
 * just very nice and easy to use.
 *
 * chan provides three primary types which form the foundations.
 *
 *     -- Send: provides send() and close(). After a Send is closed, no more messages can be
 *          sent on it.
 *
 *     -- Receive: provides receive() and closed(). After a channel is closed, any attempt to
 *          receive will return null.
 *
 *     -- Chan = Send + Receive
 *
 * BasicChan is an implementation of Chan which uses a LinkedBlockingQueue internally. There
 * are also other implementations which provide various services. For example, HistorySend and
 * HistoryReceive keep track of the history of messages as well, and RateLimitedSend allows
 * the user to provide a way of limiting the rate of the channel.
 *
 * Created by Daniel Krawisz on 3/3/16.
 *
 */
package com.shuffle.chan;
