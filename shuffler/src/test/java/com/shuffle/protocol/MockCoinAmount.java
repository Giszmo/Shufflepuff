package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/7/15.
 */
public class MockCoinAmount implements Coin.CoinAmount {
    int amount;

    MockCoinAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public boolean greater(Coin.CoinAmount ν) throws InvalidImplementationError {
        if (!(ν instanceof MockCoinAmount)) {
            throw new InvalidImplementationError();
        }

        return amount > ((MockCoinAmount)ν).amount;
    }

    @Override
    public String toString() {
        return "" + amount + " BTC";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof MockCoinAmount)) {
            return false;
        }

        return amount == ((MockCoinAmount)o).amount;
    }
}
