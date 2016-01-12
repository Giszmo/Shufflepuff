package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/7/15.
 */
class ValueException extends Exception {
    public static enum Values{session, phase, sender, recipient}

    Values value;
    String expected;
    String found;

    ValueException(Values value, String expected, String found) {
        this.value = value;
        this.expected = expected;
        this.found = found;
    }

    ValueException(Values value) {
        this.value = value;
        this.expected = null;
        this.found = null;
    }
}
