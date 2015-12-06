package com.shuffle.form;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * TO BE IMPLEMENTED BY THE USER
 *
 * This interface provides service to the Bitcoin (or other) network. This includes queries to the block
 * chain as well as to the p2p network. If these services cannot be provided while the protocol
 * is running, then the protocol must not be run.
 *
 * Created by Daniel Krawisz on 12/5/15.
 *
 */
public interface Coin {
    CoinTransaction transaction(List<VerificationKey> inputs, LinkedHashMap<VerificationKey, CoinAmount> outputs);
    void send(CoinTransaction t);
    boolean unspent(VerificationKey vk);
    CoinAmount valueHeld(VerificationKey vk) throws BlockChainException, MempoolException;
}
