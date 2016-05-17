/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 * Package p2p provides for the communication channel between two independent agents, possibly
 * with encryption or other cryptographic services.
 *
 * p2p provides five basic types with which to interact with a channel:
 *
 *    -- Channel
 *    -- Connection
 *    -- Peer
 *    -- Session
 *    -- Listener.
 *
 * Four of these are provided by the implementer
 *
 *    Channel: Channel represents a means of communication. For example, you could have TcpChannel
 *      or WebsocketClientChannel or TorChannel. You could also have something more abstract like
 *      OtrChannel or SignedChannel which are cryptographic services. It provides methods getPeer
 *      and open.
 *
 *    Connection: Connection represents an open channel, one that can actually be used for
 *      communication. It provides methods identity, close, and closed.
 *
 *    Peer: Peer represents a remote computer that can be contacted through the channel which
 *      created it via an address given by the user.
 *
 *    Session: represents an open session with a remote peer.
 *
 *    Listener: this class is implemented by the user, and it defines the behavior of the channel
 *      when a remote peer initiates a connection.
 *
 * HOW TO USE
 *
 *   * First we create an object that implements Listener and provide it to the Channel in order
 *     to open the channel. The channel provides us with a Connection object in return, signifying
 *     that the channel is opened.
 *
 *   * Then get a Peer object by giving the Channel an Address. For exmaple, for TcpChannel,
 *     Address would be an InetSocketAddress.
 *
 *   * If the channel is open, it should be possible to create a Session object from the Peer
 *     object by giving it a Send object. The Session will provide any message received by the
 *     remote peer to the Send object's send method. Now you can send messages to the remote peer
 *     with the Session.
 *
 *   * This is how to implement the Listener. When we opened a channel, it was necessary to provide
 *     a Send object in order to provide behavior when a new message is received. It is also
 *     necessary to do something like that for sessions which are initiated with remote peers. We
 *     need an automatic way of producing more Send objects when remote peers try to talk to us.
 *     That is what the listener is for. It takes a new Session object and does whatever we want
 *     to do with new sessions, and it returns a new Send object, which will be used whenever a new
 *     message is received from that peer.
 *
 * Created by Daniel Krawisz on 1/25/16.
 *
 *
 */
package com.shuffle.p2p;
