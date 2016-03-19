package com.shuffle.monad;

/**
 * Represents a type that is either X or Y.
 *
 * Created by Daniel Krawisz on 3/18/16.
 */
public class Either<X, Y> {
    public final X x;
    public final Y y;

    public Either(X x, Y y) {
        if (x == null) {
            if (y == null) {
                throw new NullPointerException();
            }
        } else if (y != null) {
            throw new IllegalArgumentException();
        }

        this.x = x;
        this.y = y;
    }
}
