package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.BitcoinCrypto;
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
public class SigningKeyImpl extends SigningKey {

   ECKey privateKey;
   BitcoinCrypto bitcoinCrypto = new BitcoinCrypto();

   public SigningKeyImpl(org.bitcoinj.core.ECKey ecKey) {
      this.privateKey = ecKey;
   }


   @Override
   public VerificationKey VerificationKey() throws CryptographyError {
      return new VerificationKeyImpl(privateKey);
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
      if (!(o instanceof SigningKeyImpl) && o.getClass() == this.getClass()) {
         throw new IllegalArgumentException("unable to compare with other SingingKey");
      }
      //get netParams to create correct address and check by address.
      org.bitcoinj.core.Address a = ((SigningKeyImpl) o).privateKey.toAddress(bitcoinCrypto.getParams());
      return a.compareTo(((org.bitcoinj.core.Address) o));
   }
}
