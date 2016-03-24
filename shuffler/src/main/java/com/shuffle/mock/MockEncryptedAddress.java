/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.mock;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.EncryptionKey;

/**
 * Created by Daniel Krawisz on 12/19/15.
 */
public class MockEncryptedAddress implements Address {
    public final Address encrypted;
    public final EncryptionKey key;

    public MockEncryptedAddress(Address encrypted, EncryptionKey key) {
        this.encrypted = encrypted;
        this.key = key;
    }

    @Override
    public String toString() {
        return "encrypted[" + encrypted.toString() + ", " + key.toString() + "]";
    }

    @Override
    public int compareTo(Address address) {
        if (address instanceof MockAddress || address instanceof MockDecryptedAddress) {
            return -1;
        }

        if (!(address instanceof MockEncryptedAddress)) {
            return 0;
        }

        return encrypted.compareTo(((MockEncryptedAddress) address).encrypted);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof MockEncryptedAddress)) {
            return false;
        }

        MockEncryptedAddress enc = (MockEncryptedAddress)o;


        return encrypted.equals(enc.encrypted) && key.equals(enc.key);
    }

    @Override
    public int hashCode() {

        int hash = 13 * encrypted.hashCode();

        if (hash < 0) {
            hash = -hash;
        }

        return hash + 17 * key.hashCode();
    }
}
