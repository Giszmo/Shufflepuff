package com.shuffle.protocol;


/**
 * Created by Daniel Krawisz on 12/3/15.
 */
public interface VerificationKey {
    boolean verify(Coin.CoinTransaction t, CoinSignature sig) throws InvalidImplementationError;
    boolean equals(Object vk);

    // Get the cryptocurrency address corresponding to this public key.
    Coin.CoinAddress address();
}
