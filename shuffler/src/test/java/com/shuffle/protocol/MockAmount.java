package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/7/15.
 */
public class MockAmount implements Coin.Amount {
    int amount;

    MockAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public boolean greater(Coin.Amount ν) throws InvalidImplementationError {
        if (!(ν instanceof MockAmount)) {
            throw new InvalidImplementationError();
        }

        return amount > ((MockAmount)ν).amount;
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

        if (!(o instanceof MockAmount)) {
            return false;
        }

        return amount == ((MockAmount)o).amount;
    }
}
