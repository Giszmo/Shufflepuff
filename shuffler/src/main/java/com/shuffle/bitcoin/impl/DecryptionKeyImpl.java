package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.BitcoinCrypto;
import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.protocol.FormatException;

import org.bitcoinj.core.ECKey;
import org.spongycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;


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


   // returns private key in Private Key WIF (compressed, 52 characters base58, starts with a 'c')
   public java.lang.String toString() {
      return this.key.getPrivateKeyAsWiF(bitcoinCrypto.getParams());
   }

   public ECKey getKey() {
      return ECKey.fromASN1(key.toASN1());
   }

   @Override
   public EncryptionKey EncryptionKey() {
      ECKey pubkey = ECKey.fromPublicOnly(encryptionKey);
      return new EncryptionKeyImpl(pubkey);
   }


   @Override
   public Address decrypt(Address m) throws FormatException {
      java.lang.String input = m.toString();
      AddressImpl returnAddress = null;
      if (bitcoinCrypto.isValidAddress(input)) {
         return new AddressImpl(input);
      } else {
         try {
            KeyFactory kf = KeyFactory.getInstance("EC");
            PrivateKey privateKey = kf.generatePrivate(kf.getKeySpec((Key) key, KeySpec.class));

            //encrypt cipher
            Cipher cipher = Cipher.getInstance("EC");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] bytes = m.toString().getBytes(StandardCharsets.UTF_8);
            byte[] decrypted = cipher.doFinal(bytes);
            returnAddress = new AddressImpl(Hex.toHexString(decrypted));

         } catch (Exception e) {
            e.printStackTrace();

         }
      }
      return returnAddress;
   }

}
