package com.shuffle.cryptocoin;


import com.shuffle.cryptocoin.Address;
import com.shuffle.cryptocoin.Signature;
import com.shuffle.cryptocoin.Transaction;
import com.shuffle.protocol.InvalidImplementationError;

/**
 *
 * Should be comparable according to the lexicographic order of the address corresponding to the keys.
 *
 * Created by Daniel Krawisz on 12/3/15.
 */
public interface VerificationKey extends Comparable {
    boolean verify(Transaction t, Signature sig) throws InvalidImplementationError;
    boolean equals(Object vk);

    // Get the cryptocurrency address corresponding to this public key.
    Address address();
}
