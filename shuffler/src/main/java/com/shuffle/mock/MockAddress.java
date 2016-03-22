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
 * Created by Daniel Krawisz on 12/19/15.
 */
public class MockAddress implements Address, Serializable {
    final public int index;

    public MockAddress(int index) {
        this.index = index;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof MockAddress)) {
            return false;
        }

        return index == ((MockAddress)o).index;
    }

    @Override
    public int hashCode() {
        return index;
    }

    @Override
    public String toString(){
        return "ad[" + index + "]";
    }

    @Override
    public int compareTo(Address address) {
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
