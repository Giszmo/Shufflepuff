package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/6/15.
 */
public class MockSessionIdentifier implements SessionIdentifier {
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MockSessionIdentifier)) {
            return false;
        }

        MockSessionIdentifier s = (MockSessionIdentifier)o;

        if (this == s) {
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return "τ[]";
    }
}
