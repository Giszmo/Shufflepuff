package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.BitcoinCrypto;
import com.shuffle.bitcoin.Signature;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.Packet;

import org.bitcoinj.core.ECKey;

/**
 * Created by conta on 31.03.16.
 */
public class VerificationKeyImpl implements VerificationKey {

   private ECKey ecKey;
   byte[] vKey;
   Address address;
   BitcoinCrypto bitcoinCrypto = new BitcoinCrypto();

   public VerificationKeyImpl(byte[] ecKey) {
      this.ecKey = ECKey.fromPublicOnly(ecKey);
      this.vKey = this.ecKey.getPubKey();
   }

   public String toString() {
      return this.vKey.toString();
   }

   @Override
   public boolean verify(Transaction t, Signature sig) throws InvalidImplementationError {
      org.bitcoinj.core.Transaction transaction = t.hashCode()
      return false;
   }

   @Override
   public boolean verify(Packet packet, Signature sig) {
      return false;
   }

   @Override
   public boolean equals(Object vk) {

      VerificationKey oKey = (VerificationKey) vk;
      return this.address == oKey.address() && oKey.getClass() == this.getClass();
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
