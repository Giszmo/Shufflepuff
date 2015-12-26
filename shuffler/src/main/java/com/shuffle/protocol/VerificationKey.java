package com.shuffle.protocol;


/**
 * TO BE IMPLEMENTED BY THE USER
 *
 * Should be comparable according to the lexicographic order of the address corresponding to the keys.
 *
 * Created by Daniel Krawisz on 12/3/15.
 */
public interface VerificationKey extends Comparable {
    boolean verify(Coin.Transaction t, Coin.Signature sig) throws InvalidImplementationError;
    boolean equals(Object vk);

    // Get the cryptocurrency address corresponding to this public key.
    Coin.Address address();
}
