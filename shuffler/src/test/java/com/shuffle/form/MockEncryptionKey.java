package com.shuffle.form;

/**
 * TODO
 *
 * Created by Daniel Krawisz on 12/8/15.
 */
public class MockEncryptionKey implements EncryptionKey {
    int index;

    public MockEncryptionKey(int index) {
        this.index = index;
    }

    @Override
    public void encrypt(Packet m) throws CryptographyException, InvalidImplementationException, FormatException {
        if (!(m instanceof MockPacket)) {
            throw new InvalidImplementationException();
        }

        MockPacket p = (MockPacket)m;

        if (p.atoms.size() != 1) {
            throw new FormatException();
        }

        MockPacket.Atom data = p.atoms.remove();

        p.atoms.remove();
        p.atoms.add(new MockPacket.Atom(new MockPacket.Encrypted(this, data)));
    }
}
