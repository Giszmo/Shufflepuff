package com.shuffle.protocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 *
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MockCoin implements Coin {
    public static class Output {
        Amount amountHeld;
        boolean spent;

        public Output(Amount amount, boolean spent) {
            this.amountHeld = amount;
            this.spent = spent;
        }

        @Override
        public String toString() {
            return "output[" + amountHeld.toString() + (spent ? "; spent" : "") + "]";
        }
    }

    ConcurrentLinkedQueue<Transaction> sentList;
    ConcurrentHashMap<Address, Output> blockchain;

    public MockCoin(Map<Address, Output> blockchain) {
        this.sentList = new ConcurrentLinkedQueue<>();
        this.blockchain = new ConcurrentHashMap<>();
        this.blockchain.putAll(blockchain);
    };

    public MockCoin() {
        this.sentList = new ConcurrentLinkedQueue<>();
        this.blockchain = new ConcurrentHashMap<>();
    }

    public synchronized void put(Address addr, Output entry) {
        blockchain.put(addr, entry);
    }

    @Override
    public Transaction shuffleTransaction(Amount ν, List<Address> inputs, Queue<Address> shuffledOutputs, Map<VerificationKey, Address> changeOutputs) {
        LinkedHashMap<Address, Amount> outputs = new LinkedHashMap<>();
        for(Address output : shuffledOutputs) {
            outputs.put(output, ν);
        }
        for(Address output : changeOutputs.values()) {
            outputs.put(output, ν); // TODO put the correct amount here.
        }
        return new MockTransaction(inputs, outputs);
    }

    @Override
    public synchronized void send(Transaction t) {
        sentList.add(t);
    }


    @Override
    public synchronized Amount valueHeld(Address addr) {
        Output entry = blockchain.get(addr);
        if (entry == null || entry.spent) {
            return new MockAmount(0);
        }

        return entry.amountHeld;
    }

    @Override
    public Transaction getOffendingTransaction(Address addr, Amount ν) {
        return null; // TODO
    }
}
