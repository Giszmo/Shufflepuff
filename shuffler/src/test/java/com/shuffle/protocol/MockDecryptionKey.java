package com.shuffle.protocol;

import com.shuffle.cryptocoin.Address;
import com.shuffle.cryptocoin.CryptographyError;
import com.shuffle.cryptocoin.DecryptionKey;
import com.shuffle.cryptocoin.EncryptionKey;

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
    // Intended blockchain decrypt a single element.
    public Address decrypt(Address m) throws FormatException, CryptographyError {

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
