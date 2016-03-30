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
 * Created by Daniel Krawisz on 2/7/16.
 */
public class MockDecryptedAddress implements Address {
    public final Address decrypted;
    public final EncryptionKey key;

    public MockDecryptedAddress(Address decrypted, EncryptionKey key) {
        this.decrypted = decrypted;
        this.key = key;
    }

    @Override
    public String toString() {
        return "decrypted[" + decrypted.toString() + ", " + key.toString() + "]";
    }

    @Override
    public int compareTo(Address address) {
        if (address instanceof MockAddress || address instanceof MockEncryptedAddress) {
            return 1;
        }

        if (!(address instanceof MockDecryptedAddress)) {
            return 0;
        }

        return decrypted.compareTo(((MockDecryptedAddress) address).decrypted);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof MockDecryptedAddress)) {
            return false;
        }

        MockDecryptedAddress dec = (MockDecryptedAddress)o;


        return decrypted.equals(dec.decrypted) && key.equals(dec.key);
    }

    @Override
    public int hashCode() {
        int hash = 11 * decrypted.hashCode();

        if (hash < 0) {
            hash = -hash;
        }

        return hash + 19 * key.hashCode();
    }
}
