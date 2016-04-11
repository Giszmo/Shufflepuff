package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.bitcoin.Signature;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.Packet;

import org.bitcoinj.core.ECKey;

/**
 * Created by conta on 10.03.16.
 */
public class SigningKeyImpl implements SigningKey {

   final ECKey privateKey;
   private VerificationKey verificationKey;

   public SigningKeyImpl(org.bitcoinj.core.ECKey ecKey) {
      this.privateKey = ecKey;
   }


   @Override
   public VerificationKey VerificationKey() throws CryptographyError {
      return new VerificationKeyImpl(privateKey.getPubKey());
   }

   @Override
   public Signature makeSignature(Transaction t) throws CryptographyError {
      return null;
   }

   @Override
   public Signature makeSignature(Packet p) throws CryptographyError {
      return null;
   }

   @Override
   public int compareTo(Object o) {
      return 0;
   }
}
