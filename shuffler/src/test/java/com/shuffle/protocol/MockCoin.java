package com.shuffle.protocol;

import com.shuffle.cryptocoin.Address;
import com.shuffle.cryptocoin.Coin;
import com.shuffle.cryptocoin.Transaction;
import com.shuffle.cryptocoin.VerificationKey;

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
        long amountHeld;
        boolean spent;

        public Output(long amount, boolean spent) {
            this.amountHeld = amount;
            this.spent = spent;
        }

        @Override
        public String toString() {
            return "output[" + amountHeld + (spent ? "; spent" : "") + "]";
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
    public Transaction shuffleTransaction(long amount, List<Address> inputs, Queue<Address> shuffledOutputs, Map<VerificationKey, Address> changeOutputs) {
        LinkedHashMap<Address, Long> outputs = new LinkedHashMap<>();
        for(Address output : shuffledOutputs) {
            outputs.put(output, amount);
        }
        for(Address output : changeOutputs.values()) {
            outputs.put(output, amount); // TODO put the correct amount here.
        }
        return new MockTransaction(inputs, outputs);
    }

    @Override
    public synchronized void send(Transaction t) {
        sentList.add(t);
    }


    @Override
    public synchronized long valueHeld(Address addr) {
        Output entry = blockchain.get(addr);
        if (entry == null || entry.spent) {
            return 0;
        }

        return entry.amountHeld;
    }

    @Override
    public Transaction getOffendingTransaction(Address addr, long ν) {
        return null; // TODO
    }

    @Override
    public boolean isOffendingTransaction(Address addr, Transaction t) {
        return false; // TODO
    }
}
