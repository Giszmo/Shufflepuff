package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.BitcoinCrypto;
import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.protocol.FormatException;

import org.bitcoinj.core.ECKey;


/**
 * A private key used for decryption.
 */

public class DecryptionKeyImpl implements DecryptionKey {

   final ECKey key;
   final byte[] encryptionKey;

   BitcoinCrypto bitcoinCrypto = new BitcoinCrypto();

   public DecryptionKeyImpl(org.bitcoinj.core.ECKey key) {
      this.key = key;
      this.encryptionKey = key.getPubKey();
   }

   @Override
   public EncryptionKey EncryptionKey() {
      return new EncryptionKeyImpl(key);
   }

   //not sure if that is meant to be passing a Message m?
   @Override
   public Address decrypt(Address m) throws FormatException {
      String input = m.toString();
      if (bitcoinCrypto.isValidAddress(input)) {
         return new AddressImpl(input);
      } else {
         // todo: string is encoded bits of address
         return null;

      }
   }
}
