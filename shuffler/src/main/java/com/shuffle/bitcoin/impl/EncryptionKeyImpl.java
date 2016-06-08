package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.BitcoinCrypto;
import com.shuffle.bitcoin.EncryptionKey;

import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.core.ECKey;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.util.Base64;

import javax.crypto.Cipher;

/**
 * Created by conta on 01.04.16.
 */
public class EncryptionKeyImpl implements EncryptionKey {

   ECKey encryptionKey;

   public EncryptionKeyImpl(byte[] ecPubKey) {
      this.encryptionKey = ECKey.fromPublicOnly(ecPubKey);
   }

   public EncryptionKeyImpl(ECKey ecPubKey) {
      if (ecPubKey.hasPrivKey()) {
         this.encryptionKey = ECKey.fromPublicOnly(ecPubKey.getPubKey());
      } else {
         this.encryptionKey = (ecPubKey);
      }
   }

   public String toString() {
      return new String(this.encryptionKey.getPublicKeyAsHex().toString());
   }

   @Override
   public Address encrypt(Address m) {
      AddressImpl add = null;
      try {
         KeyFactory kf = KeyFactory.getInstance("EC");

         //cast will fail, maybe
         //X509EncodedKeySpec spec = kf.getKeySpec(encryptionKey,X509EncodedKeySpec.class);
         //PublicKey pubKey = kf.generatePublic(kf.getKeySpec(((Key) encryptionKey), KeySpec.class));
         PublicKey publicKey = BitcoinCrypto.loadPublicKey(Base64.getEncoder().encodeToString(encryptionKey.getPrivKeyBytes()));
         byte[] publicKey2 = ECKey.publicKeyFromPrivate(encryptionKey.getPrivKey(), encryptionKey.isCompressed());

         //encrypt cipher
         Cipher cipher = Cipher.getInstance("EC");
         cipher.init(Cipher.ENCRYPT_MODE, publicKey1);
         byte[] bytes = m.toString().getBytes(StandardCharsets.UTF_8);
         byte[] encrypted = cipher.doFinal(bytes);
         add = new AddressImpl(Hex.encodeHexString(encrypted));
      } catch (Exception e) {
         e.printStackTrace();

      }
      return add;
   }

}