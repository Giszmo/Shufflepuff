package com.shuffle.protocol;

/**
 * TO BE IMPLEMENTED BY THE USER
 *
 * Represents an amount of Bitcoin or other cryptocurrency.
 *
 * Created by Daniel Krawisz on 12/3/15.
 *
 */
public interface CoinAmount {
    boolean greater(CoinAmount ν) throws InvalidImplementationException;
}
