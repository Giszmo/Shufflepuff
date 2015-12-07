package com.shuffle.form;

/**
 * Created by Daniel Krawisz on 12/3/15.
 */
public interface SigningKey {
    VerificationKey VerificationKey();
    CoinSignature makeSignature(CoinTransaction t);
    Packet sign(Packet packet);
}
