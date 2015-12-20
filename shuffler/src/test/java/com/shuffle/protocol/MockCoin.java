package com.shuffle.protocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 *
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MockCoin implements Coin {
    public static class Output {
        CoinAmount amountHeld;
        boolean spent;

        public Output(CoinAmount amount, boolean spent) {
            this.amountHeld = amount;
            this.spent = spent;
        }

        @Override
        public String toString() {
            return "output[" + amountHeld.toString() + (spent ? "; spent" : "") + "]";
        }
    }

    ConcurrentLinkedQueue<CoinTransaction> sentList;
    ConcurrentHashMap<CoinAddress, Output> blockchain;

    public MockCoin(Map<CoinAddress, Output> blockchain) {
        this.sentList = new ConcurrentLinkedQueue<>();
        this.blockchain = new ConcurrentHashMap<>();
        this.blockchain.putAll(blockchain);
    };

    public MockCoin() {
        this.sentList = new ConcurrentLinkedQueue<>();
        this.blockchain = new ConcurrentHashMap<>();
    }

    public synchronized void put(CoinAddress addr, Output entry) {
        blockchain.put(addr, entry);
    }

    @Override
    public synchronized CoinTransaction transaction(List<CoinAddress> inputs, LinkedHashMap<CoinAddress, CoinAmount> outputs) {
        return new MockCoinTransaction(inputs, outputs);
    }

    @Override
    public synchronized void send(CoinTransaction t) {
        sentList.add(t);
    }

    @Override
    public synchronized boolean unspent(CoinAddress addr) {
        Output entry = blockchain.get(addr);
        if (entry == null) {
            return false;
        }

        return !entry.spent;
    }

    @Override
    public synchronized CoinAmount valueHeld(CoinAddress addr) {
        Output entry = blockchain.get(addr);
        if (entry == null || entry.spent) {
            return new MockCoinAmount(0);
        }

        return entry.amountHeld;
    }
}
