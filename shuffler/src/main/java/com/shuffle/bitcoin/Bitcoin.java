package com.shuffle.bitcoin;


import java.util.List;
import java.util.Map;
import java.util.Queue;

public class Bitcoin implements Coin {

	final private Blockchain blockchain;

	public Bitcoin(Blockchain blockchain) {
		this.blockchain = blockchain;
	}

	// TODO
	@Override
	public com.shuffle.bitcoin.Transaction shuffleTransaction(long amount,
										  List<VerificationKey> from,
										  Queue<Address> to,
										  Map<VerificationKey, Address> changeAddresses)
										  throws CoinNetworkError {
		
	}

	// TODO
	@Override
	public long valueHeld(Address addr) throws CoinNetworkError {
		
	}

	// TODO
	@Override
	public com.shuffle.bitcoin.Transaction getConflictingTransaction(Address addr, long amount) {
		
	}

	// TODO
	@Override
	public com.shuffle.bitcoin.Transaction getSpendingTransaction(Address addr, long amount) {

	}
	
}
