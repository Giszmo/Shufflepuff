/**
 *
 * Abstract implementation of CoinShuffle in java.
 * http://crypsys.mmci.uni-saarland.de/projects/CoinShuffle/coinshuffle.pdf
 *
 * The com.shuffle.core package implements the details discussed in section 5.3: Practical
 * Considerations. These details were seen fit in the original paper to be described in a separate
 * section, and they have been implemented in the same way. This package also provides a lot of
 * necessary checks which were assumed to take place automatically in the original paper. This
 * package was written intended to be easily readable; it was written to enable com.shuffle.form to
 * be easily readible instead.
 *
 * You should have a look at ShuffleProtocol.java to review the kinds of errors that a
 * user of this library must provide for in order to implement this protocol correctly.
 *
 * Created by Daniel Krawisz on 12/6/15.
 *
 *
 *
 */
package com.shuffle.core;