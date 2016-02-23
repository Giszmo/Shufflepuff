package com.shuffle.bitcoin;

import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.Message;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.xml.crypto.KeySelector;

import sun.net.www.http.HttpClient;


/**
 * Created by Daniel Krawisz on 12/21/15.
 */
public class Bitcoin implements Coin, Crypto {
    // Figure out which network we should connect to. Each one gets its own set of files.
    NetworkParameters params = TestNet3Params.get();
    String fileprefix;

    WalletAppKit kit = new WalletAppKit(params,new File("."),fileprefix);
    Wallet wallet = new Wallet(params);
    BlockChain blockchain = new BlockChain(params,wallet,);

    @Override
    public Transaction shuffleTransaction(long amount, List<VerificationKey> inputs, Queue<Address> shuffledOutputs, Map<VerificationKey, Address> changeOutputs) {
        return null;
    }

    @Override
    public long valueHeld(Address addr) throws CoinNetworkError {

//
////        blockexplorer.com api for lookup
////        validate address
////        /api/addr-validate/[:addr]
////        /api/addr/[:addr][?noTxList=1&noCache=1]
////        /api/addr/19SokJG7fgk8iTjemJ2obfMj14FM16nqzj
////
////        Example URL: will return value in satoshis https://blockexplorer.com/api/addr/1DeQV7NiCkUYUdz9yo6Cr4mBNUUxYravfW/balance
//        int vh;
//        OkHttpClient client = new OkHttpClient();
//        String url= "https://blockexplorer.com/api/addr/"+addr+"/balance";
//        private String run(String url) throws IOException {
//            Request request = new Request.Builder()
//                  .url(url)
//                  .build();
//
//            Response response = client.newCall(request).execute();
//            vh = Integer.parseInt(response.body().string());
//            return  response.body().string();
//        }
//        vh = Integer.parseInt(run);
//        return vh;
        return 0;
    }



    @Override
    public Transaction getConflictingTransaction(Address addr, long amount) {
        return null;
    }

    @Override
    public boolean spendsFrom(Address addr, long amount, Transaction t) {
        return false;
    }

    @Override
    public DecryptionKey makeDecryptionKey() throws CryptographyError {
        return (DecryptionKey)kit.wallet().currentReceiveKey();
    }

    @Override
    public SigningKey makeSigningKey() throws CryptographyError {
        return (SigningKey) kit.wallet().currentKey(kit.wallet().getActiveKeychain().get);
    }

    @Override
    public int getRandom(int n) throws CryptographyError, InvalidImplementationError {
        SecureRandom sr = new SecureRandom();
        return sr.nextInt(n);
    }

    @Override
    public Message hash(Message m) throws CryptographyError, InvalidImplementationError {
        return null;
    }
}
