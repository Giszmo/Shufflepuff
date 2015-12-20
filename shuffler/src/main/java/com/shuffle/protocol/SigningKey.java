package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/3/15.
 */
public interface SigningKey {
    VerificationKey VerificationKey() throws CryptographyException;
    CoinSignature makeSignature(Coin.CoinTransaction t) throws CryptographyException;
}
