package com.shuffle.bitcoin;

import com.shuffle.bitcoin.impl.DecryptionKeyImpl;
import com.shuffle.bitcoin.impl.SigningKeyImpl;
import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.Message;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChain;

import java.io.File;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;


public class BitcoinCrypto implements Crypto {

   // Figure out which network we should connect to. Each one gets its own set of files.
   NetworkParameters params = TestNet3Params.get();
   String fileprefix = "_cosh";
   WalletAppKit kit;


   // if we generate new keys no need for mnemonic
   //List<String> mnemonicCode = seed.getMnemonicCode();


   //Generate Keypair using HMAC
   KeyPairGenerator keyPG = getKeyPGen();
   KeyPair keys = getKeyPair();
   KeyPair keyPair = keyPG.generateKeyPair();
   PrivateKey privKey = keyPair.getPrivate();
   PublicKey pubKey = keyPair.getPublic();



   DeterministicSeed seed = kit.wallet().getKeyChainSeed();
   SecureRandom sr = new SecureRandom(seed.getSeedBytes());
   ECKey ecKey = new ECKey(sr);
   DeterministicKeyChain keyChain = new DeterministicKeyChain(sr, 256);

   //using bitcoinj
   org.bitcoinj.core.Address pvK = kit.wallet().freshAddress(KeyChain.KeyPurpose.AUTHENTICATION);
   //using ECkey
   org.bitcoinj.core.Address ecaddress = ecKey.toAddress(params);

   public NetworkParameters getParams() {
      return params;
   }

   public void initKit() {
      //initialize files and stuff here, add our address to the watched ones
      kit = new WalletAppKit(params, new File("."), fileprefix);
      kit.setAutoSave(true);
      kit.connectToLocalHost();
      kit.useTor();
      kit.startAsync();
      kit.awaitRunning();
      kit.peerGroup().addPeerDiscovery(new DnsDiscovery(params));
   }

   public boolean isValidAddress(String address) {
      org.bitcoinj.core.Address paddress;
      try {
         paddress = new org.bitcoinj.core.Address(params, address);
      } catch (AddressFormatException e) {
         return false;
      }
      return true;
   }

   private KeyPairGenerator getKeyPGen() {
      if (keyPG == null) {
         try {
            keyPG = KeyPairGenerator.getInstance("HMacSP800DRBG");
         } catch (NoSuchAlgorithmException exception) {
            exception.printStackTrace();
         }
         final int keylength = 256;
         keyPG.initialize(keylength, sr);
         return keyPG;
      }
      return keyPG;
   }

   private KeyPair getKeyPair() {
      if (keys == null || keyPG == null) {
         if (keyPG == null) {
            keyPG = getKeyPGen();
         }

         keys = keyPG.generateKeyPair();
         return keys;
      }
      return keys;
   }



    @Override
    public DecryptionKey makeDecryptionKey() {
       ECKey newDecKey = new ECKey(sr);
       kit.wallet().importKey(newDecKey);
       return new DecryptionKeyImpl(new ECKey(sr));
    }

    @Override
    public SigningKey makeSigningKey() {
       ECKey newSignKey = new ECKey(sr);
       kit.wallet().importKey(newSignKey);
       return new SigningKeyImpl(newSignKey);
    }

    @Override
    public int getRandom(int n) throws InvalidImplementationError {
         return sr.nextInt(n);
    }

    @Override
    public Message hash(Message m) throws CryptographyError, InvalidImplementationError {
       // use KeyCrypter to encrypt message: encrypt(byte[] plainBytes, org.spongycastle.crypto.params.KeyParameter aesKey)
       try {
          MessageDigest md = MessageDigest.getInstance("SHA-256");
          String message = m.toString();
          md.update(message.getBytes());
          byte[] mHash = md.digest();
          //TODO
          return null;


       } catch (NoSuchAlgorithmException e) {
          e.printStackTrace();
       }


       // get the message bytes[] to encode

             //get the key to encrypt to, attached to message
             //      keyCrypter.encrypt();
       return null;
    }


}
