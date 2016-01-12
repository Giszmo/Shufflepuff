package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/6/15.
 */
public class MockSessionIdentifier implements SessionIdentifier {
    static String version = "CoinShuffle mock implementation for testing.";
    String id;

    public MockSessionIdentifier() {
        id = "000";
    }

    public MockSessionIdentifier(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MockSessionIdentifier)) {
            return false;
        }

        MockSessionIdentifier s = (MockSessionIdentifier)o;

        if (this == s) {
            return true;
        }

        if (version.equals(s.version) && id.equals(s.id)) {
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return "session[" + id + "]";
    }

    @Override
    public String protocol() {
        return "test protocol";
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public String id() {
        return id;
    }
}
