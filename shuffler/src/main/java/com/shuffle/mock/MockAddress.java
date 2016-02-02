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
}
