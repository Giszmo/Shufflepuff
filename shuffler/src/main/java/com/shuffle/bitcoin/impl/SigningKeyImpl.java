package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.BitcoinCrypto;
import com.shuffle.bitcoin.Signature;
import com.shuffle.bitcoin.SignatureImpl;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.bitcoin.blockchain.Bitcoin;
import com.shuffle.protocol.message.Packet;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.store.BlockStoreException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Created by conta on 10.03.16.
 */
public class SigningKeyImpl implements SigningKey {

   ECKey signingKey;
   BitcoinCrypto bitcoinCrypto = new BitcoinCrypto();

   public SigningKeyImpl(org.bitcoinj.core.ECKey ecKey) {
      this.signingKey = ecKey;
   }

   public String toString() {
      return this.signingKey.getPrivateKeyAsWiF(bitcoinCrypto.getParams()).toString();
   }

   @Override
   public VerificationKey VerificationKey() {
      return new VerificationKeyImpl(signingKey.getPubKey());
   }


   @Override
   public Signature makeSignature(Transaction t) {
      SignatureImpl signature1;
      signature1 = null;
      try {
         Bitcoin.Transaction tj = (Bitcoin.Transaction) t;
         ECKey.ECDSASignature signature = signingKey.sign(tj.bitcoinj().getHash());
         byte[] signed = signature.encodeToDER();
         signature1 = new SignatureImpl(signed);
      } catch (BlockStoreException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }
      return signature1;
   }

   @Override
   public Signature makeSignature(Packet p) {
      String input = p.toString();
      ECKey.ECDSASignature signature = signingKey.sign(Sha256Hash.twiceOf(input.getBytes(StandardCharsets.UTF_8)));
      byte[] signed = signature.encodeToDER();
      SignatureImpl signature1 = new SignatureImpl(signed);
      return signature1;
   }


   @Override
   public int compareTo(Object o) {
      if (!(o instanceof SigningKeyImpl) && o.getClass() == this.getClass()) {
         throw new IllegalArgumentException("unable to compare with other SingingKey");
      }
      //get netParams to create correct address and check by address.
      org.bitcoinj.core.Address a = ((SigningKeyImpl) o).signingKey.toAddress(bitcoinCrypto.getParams());
      return a.compareTo(((org.bitcoinj.core.Address) o));
   }
}
