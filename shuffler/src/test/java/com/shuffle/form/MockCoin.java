package com.shuffle.form;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MockCoin implements Coin {
    @Override
    public CoinTransaction transaction(List<VerificationKey> inputs, LinkedHashMap<VerificationKey, CoinAmount> outputs) {
        return null;
    }

    @Override
    public void send(CoinTransaction t) {

    }

    @Override
    public boolean unspent(VerificationKey vk) {
        return false;
    }

    @Override
    public CoinAmount valueHeld(VerificationKey vk) {
        return null;
    }
}
