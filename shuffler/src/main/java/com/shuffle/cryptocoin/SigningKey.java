package com.shuffle.cryptocoin;

import com.shuffle.cryptocoin.CryptographyError;
import com.shuffle.cryptocoin.Signature;
import com.shuffle.cryptocoin.Transaction;
import com.shuffle.cryptocoin.VerificationKey;

/**
 * Created by Daniel Krawisz on 12/3/15.
 */
public interface SigningKey {
    VerificationKey VerificationKey() throws CryptographyError;
    Signature makeSignature(Transaction t) throws CryptographyError;
}
