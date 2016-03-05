package com.shuffle.bitcoin.blockchain;


import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Coin;
import com.shuffle.bitcoin.CoinNetworkError;
import com.shuffle.bitcoin.VerificationKey;

import org.bitcoinj.store.BlockStoreException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public abstract class Bitcoin implements Coin {

	public class Transaction implements com.shuffle.bitcoin.Transaction {
        final String hash;
        private org.bitcoinj.core.Transaction bitcoinj;

        public Transaction(String hash) {
            this.hash = hash;
        }

        // Get the underlying bitcoinj representation of this transaction.
        public org.bitcoinj.core.Transaction bitcoinj() throws BlockStoreException, IOException {
            if (bitcoinj == null) {
                bitcoinj = getTransaction(hash);
            }

            return bitcoinj;
        }

        @Override
        public void send() throws CoinNetworkError {
            // TODO should only work if it's a new transaction we made, not one loaded from the block chain.
        }
    }

	// TODO
	@Override
	public com.shuffle.bitcoin.Transaction shuffleTransaction(long amount,
										  List<VerificationKey> from,
										  Queue<Address> to,
										  Map<VerificationKey, Address> changeAddresses)
										  throws CoinNetworkError {
		return null;
	}

	// TODO
	@Override
	public long valueHeld(Address addr) throws CoinNetworkError {
		return 0;
	}

	// TODO
	@Override
	public com.shuffle.bitcoin.Transaction getConflictingTransaction(Address addr, long amount) {
		return null;
	}

	// TODO
	@Override
	public com.shuffle.bitcoin.Transaction getSpendingTransaction(Address addr, long amount) {
		return null;
	}

	abstract public List<Bitcoin.Transaction> getWalletTransactions(String address) throws BlockStoreException, IOException;

	abstract protected org.bitcoinj.core.Transaction getTransaction(String transactionHash) throws BlockStoreException, IOException;
}
