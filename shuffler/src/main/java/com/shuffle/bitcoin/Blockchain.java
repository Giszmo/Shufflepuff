package com.shuffle.bitcoin;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.store.BlockStoreException;

/**
 *
 *
 *
 * Created by Eugene Siegel on 3/4/16
 *
 */

public interface Blockchain {
    String[] getWalletTransactions(String address) throws Exception;

    Transaction getTransaction(String transactionHash) throws BlockStoreException, Exception;
}
