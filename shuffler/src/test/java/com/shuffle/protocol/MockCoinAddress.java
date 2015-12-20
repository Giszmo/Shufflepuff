package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/19/15.
 */
public class MockCoinAddress implements Coin.CoinAddress {
    int index;

    public MockCoinAddress(int index) {
        this.index = index;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MockCoinAddress)) {
            return false;
        }

        return index == ((MockCoinAddress)o).index;
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
