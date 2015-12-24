package com.shuffle.bitcoinj;

import com.shuffle.protocol.BlockchainError;
import com.shuffle.protocol.Coin;
import com.shuffle.protocol.CoinNetworkError;
import com.shuffle.protocol.MempoolError;
import com.shuffle.protocol.VerificationKey;

import org.bitcoinj.core.BlockChain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Created by Daniel Krawisz on 12/21/15.
 */
public class BitcoinJ implements Coin {
    BlockChain blockchain;

    @Override
    public Transaction shuffleTransaction(long amount, List<Address> inputs, Queue<Address> shuffledOutputs, Map<VerificationKey, Address> changeOutputs) {
        return null;
    }

    @Override
    public void send(Transaction t) throws CoinNetworkError {

    }

    @Override
    public long valueHeld(Address addr) throws BlockchainError, MempoolError {
        return 0;
    }

    @Override
    public Transaction getOffendingTransaction(Address addr, long amount) {
        return null;
    }
}
