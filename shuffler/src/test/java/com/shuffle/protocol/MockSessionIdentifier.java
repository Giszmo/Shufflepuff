package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/6/15.
 */
public class MockSessionIdentifier implements SessionIdentifier {
    @Override
    public boolean equals(SessionIdentifier τ) throws InvalidImplementationException {
        if (!(τ instanceof MockSessionIdentifier)) {
            throw new InvalidImplementationException();
        }

        MockSessionIdentifier s = (MockSessionIdentifier)τ;

        if (this == s) {
            return true;
        }

        return false;
    }
}
