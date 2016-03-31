/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.mock;

import com.shuffle.bitcoin.Address;

import java.io.Serializable;

/**
 * Mock Bitcoin address used for testing.
 *
 * Created by Daniel Krawisz on 12/19/15.
 */
public class MockAddress implements Address, Serializable {
    public final int index;

    public MockAddress(int index) {
        this.index = index;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof MockAddress && index == ((MockAddress) o).index;

    }

    @Override
    public int hashCode() {
        return -index;
    }

    @Override
    public String toString() {
        return "ad[" + index + "]";
    }

    @Override
    public int compareTo(Address address) {
        if (address instanceof MockDecryptedAddress) {
            return -1;
        }

        if (address instanceof MockEncryptedAddress) {
            return 1;
        }

        if (!(address instanceof MockAddress)) {
            return 0;
        }

        int index = ((MockAddress)address).index;

        if (index < this.index) {
            return -1;
        }

        if (index > this.index) {
            return 1;
        }

        return 0;
    }
}
