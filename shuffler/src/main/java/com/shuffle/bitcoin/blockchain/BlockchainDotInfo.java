package com.shuffle.bitcoin.blockchain;

import java.io.IOException;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import org.bitcoinj.core.*;
import org.bitcoinj.store.*;
import org.json.JSONTokener;
import org.json.JSONObject;


/**
 *
 *
 *
 *
 *
 * Created by Eugene Siegel on 3/4/16.
 */
public final class BlockchainDotInfo extends Bitcoin {

    String USER_AGENT = "Chrome/5.0";

    /**
     *
     * Given a wallet address, this function looks up all transactions associated with the wallet using Blockchain.info's API.
     * These "n" transaction hashes are then returned in a String array.
     *
	 */
    public List<Transaction> getWalletTransactions(String address) throws IOException {

        String url = "https://blockchain.info/rawaddr/" + address;
        URL obj = new URL(url);
        JSONTokener tokener = new JSONTokener(obj.openStream());
        JSONObject root = new JSONObject(tokener);
        List<Transaction> txhashes = new LinkedList<>();
        for (int i = 0; i < root.getJSONArray("txs").length(); i++) {
            txhashes.add(new Transaction(root.getJSONArray("txs").getJSONObject(i).get("hash").toString()));
        }
        return txhashes;

    }

    /**
     *
     * This function takes in a transaction hash and passes it to Blockchain.info's API.  After some formatting,
     * it returns a bitcoinj Transaction object using this transaction hash.
     *
     */
    public org.bitcoinj.core.Transaction getTransaction(String transactionHash) throws BlockStoreException, IOException {

        String url = "https://blockchain.info/tr/rawtx/" + transactionHash + "?format=hex";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", USER_AGENT);
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        HexBinaryAdapter adapter = new HexBinaryAdapter();
        byte[] bytearray = adapter.unmarshal(response.toString());
        NetworkParameters params = NetworkParameters.prodNet();     // prodNet is deprecated
        Context contxt = Context.getOrCreate(params);               // bitcoinj needs this for some reason
        return new org.bitcoinj.core.Transaction(params, bytearray);

    }

}
