package com.shuffle.protocol;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Created by Daniel Krawisz on 12/8/15.
 */
public class MockTransaction implements Coin.Transaction {
    List<Coin.Address> inputs;
    LinkedHashMap<Coin.Address, Long> outputs;

    public MockTransaction(List<Coin.Address> inputs, LinkedHashMap<Coin.Address, Long> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MockTransaction)) {
            return false;
        }

        MockTransaction mock = (MockTransaction)o;

        if (this == mock) {
            return true;
        }

        if (inputs.size() != mock.inputs.size()) {
            return false;
        }

        if (outputs.size() != mock.outputs.size()) {
            return true;
        }

        Iterator<Coin.Address> i1 = inputs.iterator();
        Iterator<Coin.Address> i2 = mock.inputs.iterator();

        while(i1.hasNext()) {
            Coin.Address c1 = i1.next();
            Coin.Address c2 = i2.next();

            if (!c1.equals(c2)) {
                return false;
            }
        }

        for (Map.Entry<Coin.Address, Long> output : outputs.entrySet()) {
            if (!output.getValue().equals(mock.outputs.get(output.getKey()))) {
                return false;
            }
        }

        return true;
    }
}
