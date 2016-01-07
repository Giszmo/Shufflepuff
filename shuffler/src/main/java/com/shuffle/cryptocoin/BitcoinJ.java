package com.shuffle.cryptocoin;

import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.Message;

import org.bitcoinj.core.BlockChain;

import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Created by Daniel Krawisz on 12/21/15.
 */
public class BitcoinJ implements Coin, Crypto {
    BlockChain blockchain;

    @Override
    public Transaction shuffleTransaction(long amount, List<VerificationKey> inputs, Queue<Address> shuffledOutputs, Map<VerificationKey, Address> changeOutputs) {
        return null;
    }

    @Override
    public void send(Transaction t) throws CoinNetworkError {

    }

    @Override
    public long valueHeld(Address addr) throws CoinNetworkError {
        return 0;
    }

    @Override
    public Transaction getOffendingTransaction(Address addr, long amount) {
        return null;
    }

    @Override
    public boolean isOffendingTransaction(Address addr, long amount, Transaction t) {
        return false;
    }

    @Override
    public DecryptionKey DecryptionKey() throws CryptographyError {
        return null;
    }

    @Override
    public SigningKey SigningKey() throws CryptographyError {
        return null;
    }

    @Override
    public int getRandom(int n) throws CryptographyError, InvalidImplementationError {
        return 0;
    }

    @Override
    public Message hash(Message m) throws CryptographyError, InvalidImplementationError {
        return null;
    }
}
