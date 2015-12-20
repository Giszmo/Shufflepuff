package com.shuffle.protocol;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Daniel Krawisz on 12/8/15.
 */
public class MockCoinTransaction implements Coin.CoinTransaction {
    List<Coin.CoinAddress> inputs;
    LinkedHashMap<Coin.CoinAddress, Coin.CoinAmount> outputs;

    public MockCoinTransaction(List<Coin.CoinAddress> inputs, LinkedHashMap<Coin.CoinAddress, Coin.CoinAmount> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MockCoinTransaction)) {
            return false;
        }

        MockCoinTransaction mock = (MockCoinTransaction)o;

        if (this == mock) {
            return true;
        }

        if (inputs.size() != mock.inputs.size()) {
            return false;
        }

        if (outputs.size() != mock.outputs.size()) {
            return true;
        }

        Iterator<Coin.CoinAddress> i1 = inputs.iterator();
        Iterator<Coin.CoinAddress> i2 = mock.inputs.iterator();

        while(i1.hasNext()) {
            Coin.CoinAddress c1 = i1.next();
            Coin.CoinAddress c2 = i2.next();

            if (!c1.equals(c2)) {
                return false;
            }
        }

        for (Map.Entry<Coin.CoinAddress, Coin.CoinAmount> output : outputs.entrySet()) {
            if (!output.getValue().equals(mock.outputs.get(output.getKey()))) {
                return false;
            }
        }

        return true;
    }
}
