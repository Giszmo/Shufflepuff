package com.shuffle.player;

/**
 * Created by Daniel Krawisz on 4/26/16.
 */
public interface Packet extends com.shuffle.protocol.message.Packet {
    // Need to send messages to set up the round.
    Preliminary preliminary();
}
