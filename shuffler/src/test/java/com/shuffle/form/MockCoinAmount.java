package com.shuffle.form;

/**
 * Created by Daniel Krawisz on 12/7/15.
 */
public class MockCoinAmount implements CoinAmount {
    int amount;

    MockCoinAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public boolean greater(CoinAmount ν) throws InvalidImplementationException {
        if (!(ν instanceof MockCoinAmount)) {
            throw new InvalidImplementationException();
        }

        return amount > ((MockCoinAmount)ν).amount;
    }
}
