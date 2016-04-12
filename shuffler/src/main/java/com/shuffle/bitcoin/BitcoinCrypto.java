package com.shuffle.bitcoin;

import com.shuffle.bitcoin.impl.DecryptionKeyImpl;
import com.shuffle.bitcoin.impl.SigningKeyImpl;
import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.Message;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChain;

import java.io.File;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;


public class BitcoinCrypto implements Crypto {

   // Figure out which network we should connect to. Each one gets its own set of files.
    NetworkParameters params = TestNet3Params.get();
   String fileprefix = "_cosh";
   WalletAppKit kit;


   // if we generate new keys no need for mnemonic
   //List<String> mnemonicCode = seed.getMnemonicCode();


   KeyPairGenerator keyPG = getKeyPGen();
   KeyPair keys = getKeyPair();
   DeterministicSeed seed = kit.wallet().getKeyChainSeed();
   SecureRandom sr = new SecureRandom(seed.getSeedBytes());
   ECKey ecKey = new ECKey(sr);
   DeterministicKeyChain keyChain = new DeterministicKeyChain(sr, 256);

   // KeyCrypter keyCrypter = new KeyCrypter();

   //using bitcoinj
   org.bitcoinj.core.Address pvK = kit.wallet().freshAddress(KeyChain.KeyPurpose.AUTHENTICATION);
   //using ECkey
   org.bitcoinj.core.Address ecaddress = ecKey.toAddress(params);

   public void initKit() {
      //initialize files and stuff here, add our address to the watched ones
      kit = new WalletAppKit(params, new File("."), fileprefix);
      kit.setAutoSave(true);
      kit.connectToLocalHost();
      kit.useTor();
      kit.startAsync();
      kit.awaitRunning();
      kit.peerGroup().addPeerDiscovery(new DnsDiscovery(params));
      kit.wallet().addWatchedAddress(ecKey.toAddress(params));
   }

   public KeyPairGenerator getKeyPGen() {
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

   public KeyPair getKeyPair() {
      if (keys == null || keyPG == null) {
         KeyPairGenerator keyPairGenerator = getKeyPGen();
         keys = keyPairGenerator.generateKeyPair();
         return keys;
      }
      return keys;
   }


   SigningKey signingKey = new SigningKeyImpl(ecKey);
   DecryptionKey decryptionKey = DecryptionKeyImpl(ecKey);

    @Override
    public DecryptionKey makeDecryptionKey() throws CryptographyError {
       return (DecryptionKey) keys.getPrivate();
    }

    @Override
    public SigningKey makeSigningKey() throws CryptographyError {
       return signingKey;
    }

    @Override
    public int getRandom(int n) throws CryptographyError, InvalidImplementationError, NoSuchProviderException, NoSuchAlgorithmException {
         return sr.nextInt(n);
    }

    @Override
    public Message hash(Message m) throws CryptographyError, InvalidImplementationError {
       // use KeyCrypter to encrypt message: encrypt(byte[] plainBytes, org.spongycastle.crypto.params.KeyParameter aesKey)

       // get the message bytes[] to encode

             //get the key to encrypt to, attached to message
             //      keyCrypter.encrypt();
       return null;
    }


}
