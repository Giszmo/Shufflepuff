package com.shuffle.cryptocoin;

import com.shuffle.cryptocoin.CryptographyError;
import com.shuffle.cryptocoin.Signature;
import com.shuffle.cryptocoin.Transaction;
import com.shuffle.cryptocoin.VerificationKey;

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
