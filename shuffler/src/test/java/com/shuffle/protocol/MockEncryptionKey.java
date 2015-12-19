package com.shuffle.protocol;

/**
 *
 * Created by Daniel Krawisz on 12/8/15.
 */
public class MockEncryptionKey implements EncryptionKey {
    int index;

    public MockEncryptionKey(int index) {
        this.index = index;
    }

    @Override
    public void encrypt(Message m) throws CryptographyException, InvalidImplementationException, FormatException {
        if (!(m instanceof MockMessage)) {
            throw new InvalidImplementationException();
        }

        MockMessage p = (MockMessage)m;

        if (p.atoms.size() != 1) {
            throw new FormatException();
        }

        MockMessage.Atom data = p.atoms.remove();

        p.atoms.add(new MockMessage.Atom(new MockMessage.Encrypted(this, data)));
    }

    @Override
    public String toString() {
        return "mockEncryptionKey[" + index + "]";
    }
}
