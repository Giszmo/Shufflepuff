package com.shuffle.form;

/**
 * Created by Daniel Krawisz on 12/6/15.
 */
public class MockPacketFactory implements PacketFactory {
    @Override
    public Packet make() {
        return new MockPacket(new int[]{});
    }
}
