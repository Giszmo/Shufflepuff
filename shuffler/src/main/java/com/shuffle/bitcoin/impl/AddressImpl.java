package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.BitcoinCrypto;

import org.bitcoinj.core.AddressFormatException;

/**
 * Created by conta on 10.03.16.
 */
public class AddressImpl implements Address {

   private final org.bitcoinj.core.Address address;

   public AddressImpl(org.bitcoinj.core.Address address) {
      this.address = address;
   }

   public AddressImpl(String address) {
      BitcoinCrypto bitcoinCrypto = new BitcoinCrypto();
      org.bitcoinj.core.Address address1 = null;
      try {
         address1 = new org.bitcoinj.core.Address(bitcoinCrypto.getParams(), address);
      } catch (AddressFormatException e) {
         e.printStackTrace();
      }
      this.address = address1;
   }

   public String toString() {
      return this.address.toString();
   }

   @Override
   public int compareTo(Address o) {
      if (!(o instanceof AddressImpl)) {
         throw new IllegalArgumentException("unable to compare with other address");
      }
      return address.compareTo((new AddressImpl(o.toString())).address);
   }
}
