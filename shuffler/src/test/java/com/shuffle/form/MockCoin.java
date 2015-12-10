package com.shuffle.form;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 *
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MockCoin implements Coin {
    public static class BlockchainEntry {
        CoinAmount amountHeld;
        boolean unspent;
    }

    ConcurrentLinkedQueue<CoinTransaction> sentList;
    ConcurrentHashMap<VerificationKey, BlockchainEntry> blockchain;

    @Override
    public CoinTransaction transaction(List<VerificationKey> inputs, LinkedHashMap<VerificationKey, CoinAmount> outputs) {
        return new MockCoinTransaction(inputs, outputs);
    }

    @Override
    public void send(CoinTransaction t) {
        sentList.add(t);
    }

    @Override
    public boolean unspent(VerificationKey vk) {
        BlockchainEntry entry = blockchain.get(vk);
        if (entry == null) {
            return false;
        }

        return entry.unspent;
    }

    @Override
    public CoinAmount valueHeld(VerificationKey vk) {

        BlockchainEntry entry = blockchain.get(vk);
        if (entry == null || entry.unspent) {
            return new MockCoinAmount(0);
        }

        return entry.amountHeld;
    }
}
