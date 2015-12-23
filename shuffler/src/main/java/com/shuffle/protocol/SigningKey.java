package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/3/15.
 */
public interface SigningKey {
    VerificationKey VerificationKey() throws CryptographyError;
    Coin.Signature makeSignature(Coin.Transaction t) throws CryptographyError;
}
