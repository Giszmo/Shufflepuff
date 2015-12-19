package com.shuffle.protocol;


/**
 * Created by Daniel Krawisz on 12/3/15.
 */
public interface VerificationKey {
    boolean verify(CoinTransaction t, CoinSignature sig) throws InvalidImplementationException;
    boolean equals(Object vk);
}
