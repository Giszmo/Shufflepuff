package com.shuffle.monad;

/**
 * Created by Daniel Krawisz on 3/2/16.
 */
public class SummableFutures<X> implements Summable<X> {

    @Override
    public Zero<X> zero() {
        return new SummableFutureZero<>();
    }

    @Override
    public SummableElement<X> make(X x) {
        return new DoneSummableFuture<>(x);
    }

}
