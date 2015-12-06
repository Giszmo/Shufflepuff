/**
 *
 * Abstract implementation of CoinShuffle in java.
 * http://crypsys.mmci.uni-saarland.de/projects/CoinShuffle/coinshuffle.pdf
 *
 * The purpose of this package is nothing but to DEFINE the CoinShuffle protocol in terms of
 * primitive cryptographic operations, interactions with other players, and interactions with the
 * Bitcoin network. It follows the paper referenced above, and it assumes all relevant checks are
 * performed correctly by the underlying implementations, exactly as would a mathematical paper
 * written to exposit the protocol.
 *
 * The only files in this package intended to be read are this one, SuffleMachine.java, and
 * TestShuffleMachine.java. All meaning in all other files is determined by the engineering needs of
 * writing ShuffleMachine.java and its tests. However, all files are still commented so as to
 * provide information easily accessible information to one who has specific questions.
 *
 * Created by Daniel Krawisz on 12/6/15.
 *
 *
 */
package com.shuffle.form;