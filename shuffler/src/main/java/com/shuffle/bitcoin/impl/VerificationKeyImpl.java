package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Signature;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.Packet;

import org.bitcoinj.core.NetworkParameters;

/**
 * Created by conta on 31.03.16.
 */
public class VerificationKeyImpl implements VerificationKey {

   public byte[] ecKey;

   public VerificationKeyImpl(byte[] ecKey) {
      this.ecKey = ecKey;
   }

   @Override
   public boolean verify(Transaction t, Signature sig) throws InvalidImplementationError {

      return false;
   }

   @Override
   public boolean verify(Packet packet, Signature sig) {
      return false;
   }

   @Override
   public boolean equals(Object vk) {
      return false;
   }

   @Override
   public Address address() {
      return null;
   }

   @Override
   public int compareTo(Object o) {
      if (!(o instanceof VerificationKeyImpl)) {
         throw new IllegalArgumentException("unable to compare with other VerificationKey");
      }
      //get netParams to create right address and check by address.
      org.bitcoinj.core.Address  a= ((VerificationKeyImpl) o).privateKey.toAddress(NetworkParameters.prodNet());
      return a.compareTo(((org.bitcoinj.core.Address) o));



   }
}
