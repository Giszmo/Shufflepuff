package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.BitcoinCrypto;
import com.shuffle.bitcoin.Signature;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.bitcoin.blockchain.Bitcoin;
import com.shuffle.protocol.message.Packet;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.ECKey.ECDSASignature;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.store.BlockStoreException;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by conta on 31.03.16.
 */
public class VerificationKeyImpl implements VerificationKey {

   private final ECKey ecKey;
   private final byte[] vKey;
   private final BitcoinCrypto bitcoinCrypto = new BitcoinCrypto();

   public VerificationKeyImpl(byte[] ecKey) {
      this.ecKey = ECKey.fromPublicOnly(ecKey);
      this.vKey = this.ecKey.getPubKey();
   }

   public String toString() {
      return Arrays.toString(this.vKey);
   }

   @Override
   public boolean verify(Transaction t, Signature sig) {
      Bitcoin.Transaction tj = (Bitcoin.Transaction) t;
      try {
         return ECKey.verify(tj.bitcoinj().bitcoinSerialize(), (ECDSASignature) sig, vKey);

      } catch (BlockStoreException | IOException e) {
         e.printStackTrace();
      }

      return false;
   }

   @Override
   public boolean verify(Packet packet, Signature sig) {
      String pinput = packet.toString();
      return ecKey.verify(Sha256Hash.twiceOf(pinput.getBytes()), (ECDSASignature) sig);
   }

   @Override
   public boolean equals(Object vk) {
      if (vk.getClass() == this.getClass()) {
         VerificationKey oKey = (VerificationKey) vk;
         return this.address() == oKey.address() && oKey.getClass() == this.getClass();
      }
      return false;
   }

   @Override
   public Address address() {
      return new AddressImpl(ecKey.toAddress(bitcoinCrypto.getParams()));
   }

   @Override
   public int compareTo(Object o) {
      if (!(o instanceof VerificationKeyImpl && o.getClass() == this.getClass())) {
         throw new IllegalArgumentException("unable to compare with other VerificationKey");
      }
      //get netParams to create right address and check by address.
      org.bitcoinj.core.Address a = ((VerificationKeyImpl) o).ecKey.toAddress(bitcoinCrypto.getParams());
      return a.compareTo(((org.bitcoinj.core.Address) o));



   }
}
