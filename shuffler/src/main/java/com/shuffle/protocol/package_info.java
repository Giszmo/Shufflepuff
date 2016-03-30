/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 * Abstract implementation of CoinShuffle in java.
 * http://crypsys.mmci.uni-saarland.de/projects/CoinShuffle/coinshuffle.pdf
 *
 * The purpose of this package is only to define the CoinShuffle protocol in terms of
 * primitive cryptographic operations, interactions with other players, and interactions with the
 * BitcoinCrypto network. It follows the paper referenced above, and it assumes all relevant checks are
 * performed correctly by the underlying implementations, exactly as would a mathematical paper
 * written to exposit the protocol.
 *
 * To use this protocol instantiate the class with ShuffleProtocol
 * and then run() it. The following interfaces need to be implemented in order to make it work.
 *
 * Coin
 * CoinAddress
 * CoinAmount
 * CoinSignature
 * CoinTransaction
 *
 * Crypto
 * DecryptionKey
 * EncryptionKey
 * makeSigningKey
 * VerificationKey
 *
 * SessionIdentifier
 * MessageFactory
 * Message
 *
 * Network
 *
 * Created by Daniel Krawisz on 12/6/15.
 *
 *
 */
package com.shuffle.protocol;
