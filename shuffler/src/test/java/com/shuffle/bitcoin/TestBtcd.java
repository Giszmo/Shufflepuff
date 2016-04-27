/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.bitcoin;

import org.bitcoinj.core.*;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;

import com.shuffle.bitcoin.blockchain.Bitcoin;
import com.shuffle.bitcoin.blockchain.Btcd;

import org.bitcoinj.store.BlockStoreException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

/**
 * Created by Eugene Siegel on 4/23/16.
 */

public class TestBtcd {

    NetworkParameters netParams = MainNetParams.get();
    Btcd testCase = new Btcd(netParams, 2, "admin", "pass");

    String txid = "7301b595279ece985f0c415e420e425451fcf7f684fcce087ba14d10ffec1121";
    String hexTx = "01000000014dff4050dcee16672e48d755c6dd25d324492b5ea306f85a3ab23b4df26e16e9000000008c493046022100cb6dc911ef0bae0ab0e6265a45f25e081fc7ea4975517c9f848f82bc2b80a909022100e30fb6bb4fb64f414c351ed3abaed7491b8f0b1b9bcd75286036df8bfabc3ea5014104b70574006425b61867d2cbb8de7c26095fbc00ba4041b061cf75b85699cb2b449c6758741f640adffa356406632610efb267cb1efa0442c207059dd7fd652eeaffffffff020049d971020000001976a91461cf5af7bb84348df3fd695672e53c7d5b3f3db988ac30601c0c060000001976a914fd4ed114ef85d350d6d40ed3f6dc23743f8f99c488ac00000000";
    HexBinaryAdapter adapter = new HexBinaryAdapter();
    byte[] bytearray = adapter.unmarshal(hexTx);
    Context context = Context.getOrCreate(netParams);
    org.bitcoinj.core.Transaction tx = new org.bitcoinj.core.Transaction(netParams, bytearray);

    String txid2 = "e9166ef24d3bb23a5af806a35e2b4924d325ddc655d7482e6716eedc5040ff4d";
    String hexTx2 = "010000000b3ebeda226109c1d3a14dd64dedcb534fd2c63a5164577726917088cd784b32ef340000008b483045022100d615c147411c3a0f1ccf27dc12d38eebc22c572516b9d79c2cd3d412cf9b494502200e7b5ae3af72918cbd9efecac0f36e3dfdfb8370075fe3e989db4ea470842682014104c3f46b907cee74387bfc64232d7f71c65cdabc155b7c9571e5fe2c9c8d446abb6ea1f2ad160804be86dffbad9f7ad581588a909fbea6169debafd2bf8ed30e51ffffffff422e3e67bb90f1307a08ccbe4c7166109e3ce9fd96667d42d850f0a141e567dc000000008c493046022100ad8ca661d1205ed3b2e3646774a4ad185a41074ebadbee415cf51e63b659deb1022100b3b406dd73da34349d0ccd330b2a26da0410d6f210352a044b90b4af0cb93870014104f481d877ea9efdbd772fe2d9c4ecd37bf5326f00a4df39582464d8ad4c7a025bcdc048c1ccb926d3defaf93d73ff69aa16bbde65ce3e4e613e64976957f40f69ffffffff540f74067310be8bc42ab434ba30a15107675bea26d5e9389ce426b288dd10bc4d0000008c493046022100f0e34f1aded7b51ee03ebdc00441568d5fab9b0078282024153f782bb130499a0221008aba05be9a7eb8e0016430be8b69a6452b3f6f449c424b21b99d8fd69741e9c1014104c3f46b907cee74387bfc64232d7f71c65cdabc155b7c9571e5fe2c9c8d446abb6ea1f2ad160804be86dffbad9f7ad581588a909fbea6169debafd2bf8ed30e51ffffffff6107906fe426f75d3f7d966901927e3010a4f40a7cec129cdf75cfd54af66484000000008b483045022100dbefaabe4ac16bcbc2812310eb18e088f40882c9e022f4c0acf2c2ee7481e511022002d87cc4bbc0ed05184fb43c37b15a4b6433f2779781ada56a762f2a0b308224014104e7a74ca8c6a3a82187ee1bc96017da8a8a091b789902d1ae8e742e946f0d63936e55969f6deec30b9a9ecdb0cd1bb9accabb8ebeeef9044eeb826bb9098d9302ffffffff65de42a0e4783bf7163306d053ca4b45eac69f2b5e8edbdca4f34c6f56b00f2e000000008c493046022100f3449a2e2ef15df0b05cbef24182c567b01df190b15c1c3a6d559407dfbcdc7d022100c5f75d8ff945728004eb74b2683b960d9dfc2e1caa5518e7630d096ef3a99c6c0141049ad1c50c4771a7f91702827d83a800d706ecca8de149a2fc542fd9e626b18a3913f005c79e6275429cecbf3292cd38ce4d7cabfdffff775163b31e2722f8186affffffff8b16524277417d8cfddd6c739758fbaede6b63d33ab582a3bdad1f2d2257329d000000008c493046022100c9e660c3ad1f53e9686d06302145315ba54aaf55853e8f7cb28570ad2ed6149c022100cd3f38dd4a413e7bbada662f71cb644d967ca191c526d9220969c53811a655e90141041c0e77fafcb2242f307dfb9095be8aac42951310ff676ad6b63dc5049aab67822519407eb1ad74accbe47d765d57ae3ed5a9b4b9c04c473d686716da78aea42affffffff9da09b3198be85a4b483760aed48d264596dc8e88fe323d7d28049373f324b1b000000008a473044022062e4ed57dec5ae0c44713352c4a48fcaf2e2bf50800c3edbbfb55850162decb80220037ae90e8e02b516bda399edc39f4c26f27e4aebf8bda8b6efb3f6b1fda8d3f30141041702bf764f4f286806c14a60ffa7733e55ddd4e915212139abf8cd1f5a289fc592d8053fea7b7ba446caadfe0ad81164d339f21a0fbdda95a9f023ef4baba1e0ffffffff9dc0f1bda5f3bbdafdd862b988b410e85abb29c39e037559125cd4d87f6348a6000000008b48304502205a7c536f870243da55ca4a07aa7780c67b6d439bf86bb57ce508fbc07dd4bb880221009b262940e2b27408d2c4b922c2f519e027bed76f88da21f92a68f12ce550af3a014104b7d9a34ca3c54725832015f540e8871378667340973d5b21e7433598f699b469306d28c5583ad2329bac39b895621c9a6c94f53514d921f46880037fd4dcb15dffffffffbc8f2079770eaedf4e381f335c92394d5ca805cbbfc28732489c47bc5d9f04b0000000008b48304502200c3bbebf4e73e1cf630a6e4209daf25f35e39bbb5542677eb460b577344fcb59022100f5291620e57eb7653d37caea26e1ad9292944ad07e5866cf2889c61641909f7b014104b613732bfdf26577a01ff67c10907f2109af5ffc3a162010a45c0bd3067cae436c8d742793554c3fb8afaba462fe474fc9517c9d3a202845e4f375eafc77b05affffffffd334f4a5fe857fc64d09e7420fbc8c06b330f85399667c97cfbd4b1bae19ec87000000008c493046022100f72fe6becc5f0ea0cd70f15fd5a399fcbf5d28d138a7697b42732f4558defa380221009d5aed1b48a9388c719d3fbe954d2cf46c79ae2988b31405672b28ffc07876050141040db784a8478466c148ea6606eae663557967be70a1b08b0c60a2e43474f48738aef455ca89a0c343ff7f67f918ba1d44f3229544d37383c9bc2c3ad1a675ce35ffffffffe16a64084f8b5f3bdeb262a0871ecce5cb8d5e7ff2f86c50f28b3eed768af85e370000008b4830450221008ced488fa6fffd3777e8ba7b05ea4cb10eeaca3892e50690e5111d7ec3c382eb02202e140b9a13672e0d2d1b9aae9cd6da6c4a4d8759091190bcdae3e4229cf6ad8e014104c3f46b907cee74387bfc64232d7f71c65cdabc155b7c9571e5fe2c9c8d446abb6ea1f2ad160804be86dffbad9f7ad581588a909fbea6169debafd2bf8ed30e51ffffffff0140d0f57d080000001976a9145478d152bb557ac994c9793cece77d4295ed37e388ac00000000";
    byte[] bytearray2 = adapter.unmarshal(hexTx2);
    Transaction tx2 = new Transaction(netParams, bytearray2);

    String testAddress = "18heVg1RMgPbrciP2iW42nfsTtyPrMhpkd";
    List<Transaction> txList = new LinkedList<>(Arrays.asList(tx,tx2));


    @Test
    public void testGetTransaction() throws IOException {
        Assert.assertEquals(testCase.getTransaction(txid), tx);
    }

    public void testGetWalletTransactions() throws IOException, BlockStoreException {

        /**
         * there are only two transactions for testAddress.
         * I am unable to compare the List<Bitcoin.Transaction> returned by getWalletTransactions
         * with another hard-coded test List<Bitcoin.Transaction> because you cannot initialize Bitcoin.Transaction
         * outside of the Bitcoin.java class.  So instead, I compare the transactions contained in
         * the List<Bitcoin.Transaction> that is returned, with their hard-coded Transaction representation.
         * I feel like this is a suitable enough test to see that getWalletTransactions contains
         * the correct bitcoinj Transaction objects and is working.
         */

        List<Bitcoin.Transaction> listOfTx = testCase.getAddressTransactions(testAddress);
        Transaction testTx = listOfTx.get(0).bitcoinj();
        Transaction testTx2 = listOfTx.get(1).bitcoinj();
        List<Transaction> testTxList = new LinkedList<>(Arrays.asList(testTx, testTx2));
        Assert.assertEquals(txList, testTxList);

    }

}
