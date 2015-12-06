package com.shuffle.form;

/**
 * Created by Daniel Krawisz on 12/3/15.
 */
public interface SigningKey {
    VerificationKey VerificationKey();
    CoinSignature sign(CoinTransaction t);
}
