package com.shuffle.bitcoin;

/**
 *
 * Should be comparable according to the lexicographic order of the address corresponding to the keys.
 *
 * Created by Daniel Krawisz on 12/3/15.
 */
public interface SigningKey {
    VerificationKey VerificationKey() throws CryptographyError;
    Signature makeSignature(Transaction t) throws CryptographyError;
}
