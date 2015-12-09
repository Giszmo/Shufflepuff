package com.shuffle.form;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by Daniel Krawisz on 12/8/15.
 */
public class MockCoinTransaction implements CoinTransaction {
    List<VerificationKey> inputs;
    LinkedHashMap<VerificationKey, CoinAmount> outputs;

    public MockCoinTransaction(List<VerificationKey> inputs, LinkedHashMap<VerificationKey, CoinAmount> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }
}
