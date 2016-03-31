/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.bitcoin.blockchain;


import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Coin;
import com.shuffle.bitcoin.CoinNetworkException;
import com.shuffle.bitcoin.VerificationKey;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.store.BlockStoreException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public abstract class Bitcoin implements Coin {

    final NetworkParameters netParams;
    final PeerGroup peerGroup;
    final int minPeers;

    /**
     *
     * The constructor takes in a NetworkParameters variable that determines whether we
     * are connecting to the Production Net or the Test Net.  It also takes in an int
     * which determines the minimum number of peers to connect to before broadcasting a transaction.
     *
     */

    public Bitcoin(NetworkParameters netParams, int minPeers) {
        this.netParams = netParams;
        this.minPeers = minPeers;
        peerGroup = new PeerGroup(netParams);
        peerGroup.setMinBroadcastConnections(minPeers);
        peerGroup.addPeerDiscovery(new DnsDiscovery(netParams));
        peerGroup.startAsync();
    }

    public class Transaction implements com.shuffle.bitcoin.Transaction {
        final String hash;
        private org.bitcoinj.core.Transaction bitcoinj;
        final boolean canSend;

        public Transaction(String hash, boolean canSend) {
            this.hash = hash;
            this.canSend = canSend;
        }

        public Transaction(String hash, org.bitcoinj.core.Transaction bitcoinj, boolean canSend) {
            this.hash = hash;
            this.bitcoinj = bitcoinj;
            this.canSend = canSend;
        }

        // Get the underlying bitcoinj representation of this transaction.
        public org.bitcoinj.core.Transaction bitcoinj() throws BlockStoreException, IOException {
            if (bitcoinj == null) {
                bitcoinj = getTransaction(hash);
            }

            return bitcoinj;
        }

        /**
         *
         * The send() method broadcasts a transaction into the Bitcoin network.  The canSend boolean
         * variable tells us if the transaction was created by us, or taken from the blockchain.
         * If we created the transaction, then we are able to broadcast it.  Otherwise, we cannot.
         *
         */

        @Override
        public boolean send() throws CoinNetworkException {
            if (!this.canSend) {
                return false;
            }

            peerGroup.start(); //calls a blocking start while peerGroup discovers peers
            try {
                //checks to see if transaction was broadcast
                peerGroup.broadcastTransaction(this.bitcoinj).future().get();
            } catch (Exception e) {
                throw new CoinNetworkException();
            }
            return true;
        }
    }

    // TODO
    // Take transaction fees into account

    /**
     *
     * The shuffleTransaction method returns a Bitcoin.Transaction object that contains a bitcoinj
     * Transaction member which sends "amount" satoshis from the addresses listed in the "from"
     * variable to addresses listed in the "to" variable, in their respective orders.  The bitcoinj
     * Transaction member also sends change from the addresses listed in the "from" variable to
     * addresses listed in the "changeAddresses" variable, in their respective order.
     *
     * To calculate the amount in change to send to the "changeAddresses", we first lookup the
     * transaction associated with an address. We only allow one transaction per address that wants
     * to shuffle their coins.  We then find the transaction output associated with our address,
     * and see how much value was sent to our address.  We then subtract the "amount" from this
     * value and this is the amount to send to the changeAddress.
     *
     * Note: We allow no past transactions to addresses in the "to" variable.
     *
     */

    @Override
    public Bitcoin.Transaction shuffleTransaction(long amount,
                                                  List<VerificationKey> from,
                                                  Queue<Address> to,
                                                  Map<VerificationKey, Address> changeAddresses)
            throws CoinNetworkException {


        // this section adds inputs to the transaction and adds outputs to the change addresses.
        org.bitcoinj.core.Transaction tx = new org.bitcoinj.core.Transaction(netParams);
        for (VerificationKey key : from) {
            try {
                String address = key.address().toString();
                List<Bitcoin.Transaction> transactions = getWalletTransactions(address);
                if (transactions.size() > 1) return null;
                org.bitcoinj.core.Transaction tx2 = getTransaction(transactions.get(0).hash);
                for (TransactionOutput output : tx2.getOutputs()) {
                    String addressP2pkh = output.getAddressFromP2PKHScript(netParams).toString();
                    if (address.equals(addressP2pkh)) {
                        tx.addInput(output);
                        if (!changeAddresses.containsKey(key) | changeAddresses.get(key) != null) {
                            try {
                                tx.addOutput(output.getValue().subtract(
                                                org.bitcoinj.core.Coin.SATOSHI.multiply(amount)),
                                        new org.bitcoinj.core.Address(
                                                netParams, changeAddresses.get(key).toString()));
                            } catch (AddressFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

            } catch (IOException e) {
                throw new CoinNetworkException();
            }
        }

        for (Address sendto : to) {
            String address = sendto.toString();
            try {
                List<Bitcoin.Transaction> transactions = getWalletTransactions(address);
                if (transactions.size() > 0) return null;
            } catch (IOException e) {
                throw new CoinNetworkException();
            }
            try {
                tx.addOutput(org.bitcoinj.core.Coin.SATOSHI.multiply(amount),
                        new org.bitcoinj.core.Address(netParams, address));
            } catch (AddressFormatException e) {
                e.printStackTrace();
            }
        }

        return new Transaction(tx.getHashAsString(), tx, true);
    }

    /**
     *
     * The valueHeld method takes in an Address variable and returns the balance held using
     * satoshis as the unit.
     *
     */

    @Override
    public long valueHeld(Address addr) throws CoinNetworkException {
        try {
            return getAddressBalance(addr.toString());
        } catch (IOException e) {
            throw new CoinNetworkException();
        }
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

    abstract List<Bitcoin.Transaction> getWalletTransactions(String address)
            throws IOException;

    abstract org.bitcoinj.core.Transaction getTransaction(String transactionHash)
            throws IOException;

    abstract long getAddressBalance(String address) throws IOException;
}
