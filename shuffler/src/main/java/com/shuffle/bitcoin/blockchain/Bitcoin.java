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
import com.shuffle.bitcoin.CoinNetworkError;
import com.shuffle.bitcoin.VerificationKey;


import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.net.discovery.DnsDiscovery;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public abstract class Bitcoin implements Coin {

	final NetworkParameters netParams;
	final PeerGroup peerGroup;

	public Bitcoin(NetworkParameters netParams, PeerGroup peerGroup) {
		this.netParams = netParams;
		this.peerGroup = peerGroup;
	}

	public class Transaction implements com.shuffle.bitcoin.Transaction {
		final String hash;
		private org.bitcoinj.core.Transaction bitcoinj;
		boolean canSend = false;

		public Transaction(String hash) {
			this.hash = hash;
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

		@Override
		public boolean send() throws CoinNetworkError {
			// TODO should only work if it's a new transaction we made, not one loaded from the block chain.
			if (!this.canSend) {
				return false;
			}

			peerGroup.addPeerDiscovery(new DnsDiscovery(netParams));
			peerGroup.start(); //calls a blocking start while peerGroup discovers peers
			try {
				peerGroup.broadcastTransaction(this.bitcoinj).future().get(); //checks to see if transaction was broadcast
			} catch (Exception e) {
				throw new CoinNetworkError();
			}
			return true;
		}
	}

	// TODO
	// Take transaction fees into account

	// TODO
	@Override
	public Bitcoin.Transaction shuffleTransaction(long amount,
												  List<VerificationKey> from,
												  Queue<Address> to,
												  Map<VerificationKey, Address> changeAddresses)
			throws CoinNetworkError {


		org.bitcoinj.core.Transaction tx = new org.bitcoinj.core.Transaction(netParams);
		for (VerificationKey key : from) {
			try {
				String address = key.address().toString();
				List<Bitcoin.Transaction> transactions = getWalletTransactions(address);
				if (transactions.size() > 1) return null;
				org.bitcoinj.core.Transaction tx2 = getTransaction(transactions.get(0).hash);
				for (TransactionOutput output : tx2.getOutputs()) {
					String addressP2PKH = output.getAddressFromP2PKHScript(netParams).toString();
					if (address.equals(addressP2PKH)) {
						tx.addInput(output);
						if (!changeAddresses.containsKey(key) | changeAddresses.get(key) != null) {
							try {
								tx.addOutput(output.getValue().subtract(org.bitcoinj.core.Coin.SATOSHI.multiply(amount)), new org.bitcoinj.core.Address(netParams, changeAddresses.get(key).toString()));
							} catch(AddressFormatException e) {
								e.printStackTrace();
							}
						}
					}
				}

			} catch(IOException e) {
				throw new CoinNetworkError();
			}
		}

		for (Address change : to) {
			String address = change.toString();
			try {
				List<Bitcoin.Transaction> transactions = getWalletTransactions(address);
				if (transactions.size() > 0) return null;
			} catch(IOException e) {
				throw new CoinNetworkError();
			}
			try {
				tx.addOutput(org.bitcoinj.core.Coin.SATOSHI.multiply(amount), new org.bitcoinj.core.Address(netParams, address));
			} catch(AddressFormatException e) {
				e.printStackTrace();
			}
		}

		return new Transaction(tx.getHashAsString(), tx, true);
	}

	// TODO
	@Override
	public long valueHeld(Address addr) throws CoinNetworkError {
		try {
			return getAddressBalance(addr.toString());
		} catch(IOException e) {
			throw new CoinNetworkError();
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

	abstract public List<Bitcoin.Transaction> getWalletTransactions(String address) throws IOException;

	abstract protected org.bitcoinj.core.Transaction getTransaction(String transactionHash) throws IOException;

	abstract public long getAddressBalance(String address) throws IOException;
}
