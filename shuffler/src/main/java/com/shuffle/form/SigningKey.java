package com.shuffle.form;

/**
 * Created by Daniel Krawisz on 12/3/15.
 */
public interface SigningKey {
    VerificationKey VerificationKey() throws CryptographyException;
    CoinSignature makeSignature(CoinTransaction t) throws CryptographyException;
    void sign(Packet packet) throws CryptographyException, InvalidImplementationException;
}
