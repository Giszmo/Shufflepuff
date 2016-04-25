/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.bitcoin.blockchain;

import com.shuffle.mock.MockCoin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;

import org.apache.commons.codec.binary.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by Eugene Siegel on 4/22/16.
 */

/**
 *
 * This class allows lookup of transactions associated to any Bitcoin address.
 * This in turn, allows address balance lookups for any Bitcoin address.
 * The lookups are through Btcd.
 *
 *
 * Instructions:
 *
 * In your Btcd.conf file, set an rpcuser and rpcpass (rpclimituser & rpclimitpass also works).
 * Make sure addrindex = 1 (this indexes Bitcoin addresses) and notls = 1 (currently TLS is not supported)
 *
 * Alternatively, if you do not wish to edit the config file for addrindex and notls,
 * you can use the two flags in the command line:
 * "./btcd --addrindex --notls"
 *
 */
public class Btcd extends Bitcoin {

    String rpcuser;
    String rpcpass;

    public Btcd(NetworkParameters netParams, int minPeers, String rpcuser, String rpcpass) {
        super(netParams, minPeers);
        this.rpcuser = rpcuser;
        this.rpcpass = rpcpass;
    }

    /**
     * This method takes in a transaction hash and returns a bitcoinj transaction object.
     */
    public org.bitcoinj.core.Transaction getTransaction(String transactionHash) throws IOException {

        org.bitcoinj.core.Transaction tx = null;
        String requestBody = "{\"jsonrpc\":\"2.0\",\"id\":\"null\",\"method\":\"getrawtransaction\", \"params\":[\"" + transactionHash + "\"]}";
        URL url = new URL("http://127.0.0.1:8334");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        Base64 b = new Base64();
        String authString = rpcuser + ":" + rpcpass;
        String encoding = b.encodeAsString(authString.getBytes());
        connection.setRequestProperty("Authorization", "Basic " + encoding);
        connection.setRequestProperty("Content-Length", Integer.toString(requestBody.getBytes().length));
        connection.setDoInput(true);
        OutputStream out = connection.getOutputStream();
        out.write(requestBody.getBytes());

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();

            JSONObject json = new JSONObject(response.toString());
            String hexTx = (String) json.get("result");
            HexBinaryAdapter adapter = new HexBinaryAdapter();
            byte[] bytearray = adapter.unmarshal(hexTx);
            Context context = Context.getOrCreate(netParams);
            tx = new org.bitcoinj.core.Transaction(netParams, bytearray);

        }

        out.flush();
        out.close();

        return tx;

    }

    /**
     * This method will take in an address hash and return a List of all transactions associated with
     * this address.  These transactions are in bitcoinj's Transaction format.
     */
    public List<Transaction> getAddressTransactions(String address) throws IOException {

        List<Transaction> txList = null;
        String requestBody = "{\"jsonrpc\":\"2.0\",\"id\":\"null\",\"method\":\"searchrawtransactions\", \"params\":[\"" + address + "\"]}";
        URL url = new URL("http://127.0.0.1:8334");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        Base64 b = new Base64();
        String authString = rpcuser + ":" + rpcpass;
        String encoding = b.encodeAsString(authString.getBytes());
        connection.setRequestProperty("Authorization", "Basic " + encoding);
        connection.setRequestProperty("Content-Length", Integer.toString(requestBody.getBytes().length));
        connection.setDoInput(true);
        OutputStream out = connection.getOutputStream();
        out.write(requestBody.getBytes());

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();

            JSONObject json = new JSONObject(response.toString());
            JSONArray jsonarray = json.getJSONArray("result");
            txList = new LinkedList<>();
            for (int i = 0; i < jsonarray.length(); i++) {
                JSONObject currentJson = jsonarray.getJSONObject(i);
                String txid = currentJson.get("txid").toString();
                HexBinaryAdapter adapter = new HexBinaryAdapter();
                byte[] bytearray = adapter.unmarshal(currentJson.get("hex").toString());
                Context context = Context.getOrCreate(netParams);
                org.bitcoinj.core.Transaction bitTx = new org.bitcoinj.core.Transaction(netParams, bytearray);
                Transaction tx = new Transaction(txid, bitTx, false);
                txList.add(tx);
            }

        }

        out.flush();
        out.close();

        return txList;

    }

}
