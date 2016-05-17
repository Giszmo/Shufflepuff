package com.shuffle.bitcoin;

import org.bitcoinj.core.ECKey;

import java.io.Serializable;

/**
 * Created by constantin on 17.05.16.
 */
public class SignatureImpl implements Signature, Serializable {

   //signed bitcoin transaction
   ECKey.ECDSASignature signature;

   public SignatureImpl(byte[] input) {
      this.signature = ECKey.ECDSASignature.decodeFromDER(input);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      SignatureImpl signature1 = (SignatureImpl) o;

      return signature.equals(signature1.signature);

   }
}
