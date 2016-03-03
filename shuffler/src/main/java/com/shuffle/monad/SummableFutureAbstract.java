package com.shuffle.monad;

/**
 * Created by Daniel Krawisz on 3/2/16.
 */
public abstract class SummableFutureAbstract<X> implements SummableFuture<X> {
    public Summable.SummableElement<X> plus(Summable.SummableElement<X> x) {
        if (!((x instanceof NaturalSummableFuture) || (x instanceof PlusSummableFuture))) {
            return null;
        }

        return new PlusSummableFuture<X>(this, (SummableFuture<X>) x);
    }

    public SummableFuture<X> plus(SummableFuture<X> x) {
        if (!((x instanceof NaturalSummableFuture) || (x instanceof PlusSummableFuture))) {
            return null;
        }

        return new PlusSummableFuture<X>(this, (SummableFuture<X>) x);
    }
}
