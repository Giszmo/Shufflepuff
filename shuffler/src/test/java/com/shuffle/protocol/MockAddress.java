package com.shuffle.protocol;

import com.shuffle.bitcoin.Address;

/**
 * Created by Daniel Krawisz on 12/19/15.
 */
public class MockAddress implements Address {
    int index;

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
}
