package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/3/15.
 */
public interface SigningKey {
    VerificationKey VerificationKey() throws CryptographyError;
    Coin.CoinSignature makeSignature(Coin.CoinTransaction t) throws CryptographyError;
}
