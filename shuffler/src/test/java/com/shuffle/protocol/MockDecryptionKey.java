package com.shuffle.protocol;

/**
 * TODO
 *
 * Created by Daniel Krawisz on 12/8/15.
 */
public class MockDecryptionKey implements DecryptionKey {
    final int index;
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
    public Coin.Address decrypt(Coin.Address m) throws FormatException, CryptographyError {

        if (!(m instanceof MockEncryptedAddress)) {
            throw new FormatException();
        }

        MockEncryptedAddress enc = ((MockEncryptedAddress)m);

        if (!enc.key.equals(key)) {
            throw new CryptographyError();
        }

        return ((MockEncryptedAddress)m).encrypted;
    }

    @Override
    public String toString() {
        return "dk[" + index + "]";
    }
}
