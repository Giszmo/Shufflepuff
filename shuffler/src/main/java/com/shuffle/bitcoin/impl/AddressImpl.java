package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Address;

/**
 * Created by conta on 10.03.16.
 */
public class AddressImpl implements Address {

   private final org.bitcoinj.core.Address address;

   public AddressImpl(org.bitcoinj.core.Address address) {
      this.address = address;
   }

   @Override
   public int compareTo(Address o) {
      if (!(o instanceof AddressImpl)) {
         throw new IllegalArgumentException("unable to compare with other address");
      }
      return address.compareTo(((AddressImpl) o).address);
   }
}
