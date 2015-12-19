package com.shuffle.protocol;

/**
 * TODO
 *
 * Created by Daniel Krawisz on 12/8/15.
 */
public class MockDecryptionKey implements DecryptionKey {
    int index;
    MockEncryptionKey key;

    public MockDecryptionKey(int index) {
        this.index = index;
        key = new MockEncryptionKey(index);
    }

    @Override
    public EncryptionKey EncryptionKey() {
        return key;
    }

    @Override
    // Intended to decrypt a single element.
    public void decrypt(Message m) throws InvalidImplementationException, FormatException {
        if (!(m instanceof MockMessage)) {
            throw new InvalidImplementationException();
        }

        MockMessage p = (MockMessage)m;

        if (p.atoms.size() != 1) {
            throw new FormatException();
        }

        MockMessage.Encrypted data = p.atoms.peek().encrypted;
        if (data == null || !key.equals(data.by)) {
            throw new FormatException();
        }

        p.atoms.remove();
        p.atoms.add(data.encrypted);
    }
}
