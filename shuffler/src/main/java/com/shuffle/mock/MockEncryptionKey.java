/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.mock;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.bitcoin.EncryptionKey;

import java.io.Serializable;

/**
 *
 * Created by Daniel Krawisz on 12/8/15.
 */
public class MockEncryptionKey implements EncryptionKey, Serializable {
    public final int index;

    public MockEncryptionKey(int index) {
        this.index = index;
    }

    @Override
    public Address encrypt(Address m) throws CryptographyError {
        if (m instanceof MockDecryptedAddress) {
            MockDecryptedAddress dec = (MockDecryptedAddress) m;

            if (equals(dec.key)) {
                return dec.decrypted;
            }
        }

        return new MockEncryptedAddress(m, this);
    }

    @Override
    public String toString() {
        return "ek[" + index + "]";
    }

    @Override
    public int hashCode() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        return o != null
                && o instanceof MockEncryptionKey && index == ((MockEncryptionKey) o).index;
    }

}
