package com.shuffle.bitcoinj;

import com.shuffle.protocol.BlockchainError;
import com.shuffle.protocol.Coin;
import com.shuffle.protocol.CoinNetworkError;
import com.shuffle.protocol.MempoolError;

import org.bitcoinj.core.BlockChain;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by Daniel Krawisz on 12/21/15.
 */
public class BitcoinJ implements Coin {
    BlockChain blockchain;

    @Override
    public CoinTransaction transaction(List<CoinAddress> inputs, LinkedHashMap<CoinAddress, CoinAmount> outputs) {
        return null;
    }

    @Override
    public void send(CoinTransaction t) throws CoinNetworkError {

    }

    @Override
    public CoinAmount valueHeld(CoinAddress addr) throws BlockchainError, MempoolError {
        return null;
    }

    @Override
    public CoinTransaction getOffendingTransaction(CoinAddress addr, CoinAmount ν) {
        return null;
    }
}
