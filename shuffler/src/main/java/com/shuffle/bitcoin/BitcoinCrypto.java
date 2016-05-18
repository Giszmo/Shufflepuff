package com.shuffle.bitcoin;

import com.shuffle.bitcoin.impl.DecryptionKeyImpl;
import com.shuffle.bitcoin.impl.SigningKeyImpl;
import com.shuffle.protocol.InvalidImplementationError;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.crypto.HDUtils;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.KeyChain;

import java.io.File;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;


public class BitcoinCrypto implements Crypto {

   // Figure out which network we should connect to. Each one gets its own set of files.
   NetworkParameters params = TestNet3Params.get();
   String fileprefix = "_shuffle";
   WalletAppKit kit;


   //Generate Keypair using HMAC
   KeyPairGenerator keyPG = getKeyPGen();
   KeyPair keys = getKeyPair();
   KeyPair keyPair = keyPG.generateKeyPair();
   PrivateKey privKey = keyPair.getPrivate();
   PublicKey pubKey = keyPair.getPublic();

   //DeterministicSeed seed = kit.wallet().getKeyChainSeed();
   // if we generate new keys no need for mnemonic, apparently we don't
   //List<String> mnemonicCode = seed.getMnemonicCode();
   SecureRandom sr = new SecureRandom();

   public NetworkParameters getParams() {
      return params;
   }

   // create derivation path for shuffle keys
   HDUtils hdUtils = new HDUtils();
   final String path = HDUtils.formatPath(HDUtils.parsePath("ShuffleAutH/"));
   int decKeyCounter = 0;

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
      try {
         new Address(params, address);
         return true;
      } catch (AddressFormatException e) {
         return false;
      }
   }

   private KeyPairGenerator getKeyPGen() {
      if (keyPG == null) {
         try {
            keyPG = KeyPairGenerator.getInstance("EC");
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

   public int getDecKeyCounter() {
      return decKeyCounter;
   }

   public Wallet getWallet() {
      return kit.wallet();
   }

   public String getCurrentPathAsString() {
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append(path);
      stringBuilder.append(getDecKeyCounter());
      String fpath = stringBuilder.toString();
      return fpath;
   }

    @Override
    public DecryptionKey makeDecryptionKey() {
       String ppath = getCurrentPathAsString();
       ECKey newDecKey = kit.wallet().getKeyByPath(HDUtils.parsePath(ppath));
       decKeyCounter++;
       return new DecryptionKeyImpl(newDecKey);
    }

    @Override
    public SigningKey makeSigningKey() {
       ECKey newSignKey = kit.wallet().freshKey(KeyChain.KeyPurpose.RECEIVE_FUNDS);
       return new SigningKeyImpl(newSignKey);
    }

    @Override
    public int getRandom(int n) throws InvalidImplementationError {
         return sr.nextInt(n);
    }

   /**
    * Phase 1: Key exchange
    * Each participant (except for Alice) creates an key pair of a public key encryption scheme ( makeDecryptionKey() ), consisting of a public encryption key and a private decryption key. We call the public encryption keys ekB, ekC, and ekD. Each participant announces his public encryption key (EncryptionKey), signed with the signature key (SigningKey -> VerificationKey.Address() ) corresponding to his input address.
    * <p>
    * Phase 2: Shuffling
    * Once everybody knows the public encryption key each other, the shuffling can start:
    * <p>
    * Alice encrypts her output address A' with all the encryption keys, in a layered manner. That is, Alice encrypts A' first for Dave, obtaining enc(ekD, A'). Then this ciphertext is encrypted for Charlie, obtaining enc(ekC, enc(ekD, A')) and so on for Dave. This resulting message is sent to Bob:
    * Alice ⟶ Bob: enc(ekB, enc(ekC, enc(ekD, A')))
    * <p>
    * Bob gets the message, decrypts it, obtaining enc(ekC, enc(ekD, A')).
    * He also creates a nested encryption of his address, obtaining enc(ekC, enc(ekD, B')).
    * Now Bob has a list two ciphertexts, containing A' and B'. Bob shuffles this list randomly, i.e., either exchanges the two entries or leave them. Say we are in the case that they are exchanged. Bob sends the shuffled list to Charlie:
    * Bob ⟶ Charlie: enc(ekC, enc(ekD, B')) ; enc(ekC, enc(ekD, A'))
    * <p>
    * Charlie does the same: He decrypts the two entries in the list, adds his own entry and shuffles the list:
    * Charlie ⟶ Dave: enc(ekD, B') ; enc(ekD, C') ; enc(ekD, A')
    * <p>
    * Dave does the same again: He decrypts all entries, obtaining B', C', A'. He adds his own address D' and
    * shuffles the list. The resulting shuffled list is sent to everybody:
    * Dave ⟶ everybody: D', B', C', A'
    **/



}
