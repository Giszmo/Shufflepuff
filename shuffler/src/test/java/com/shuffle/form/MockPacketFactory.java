package com.shuffle.form;

/**
 * Created by Daniel Krawisz on 12/9/15.
 */
public class MockPacketFactory implements PacketFactory {
    VerificationKey key; // The key of the person making this message.

    public MockPacketFactory(VerificationKey key) {
        this.key = key;
    }

    @Override
    public Packet make() {
        return new MockPacket(key);
    }

    public Packet makeBy(VerificationKey vk) {
        return new MockPacket(vk);
    }
}
