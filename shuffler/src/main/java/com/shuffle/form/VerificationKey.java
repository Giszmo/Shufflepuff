package com.shuffle.form;

/**
 * Created by Daniel Krawisz on 12/3/15.
 */
public interface VerificationKey extends Message {
    boolean verify(CoinTransaction t, CoinSignature sig);
    boolean equals(VerificationKey vk);
}
