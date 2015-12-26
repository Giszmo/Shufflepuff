package com.shuffle.protocol;

import com.shuffle.cryptocoin.Address;
import com.shuffle.cryptocoin.Transaction;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Daniel Krawisz on 12/8/15.
 */
public class MockTransaction implements Transaction {
    List<Address> inputs;
    LinkedHashMap<Address, Long> outputs;

    public MockTransaction(List<Address> inputs, LinkedHashMap<Address, Long> outputs) {
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

        Iterator<Address> i1 = inputs.iterator();
        Iterator<Address> i2 = mock.inputs.iterator();

        while(i1.hasNext()) {
            Address c1 = i1.next();
            Address c2 = i2.next();

            if (!c1.equals(c2)) {
                return false;
            }
        }

        for (Map.Entry<Address, Long> output : outputs.entrySet()) {
            if (!output.getValue().equals(mock.outputs.get(output.getKey()))) {
                return false;
            }
        }

        return true;
    }
}
