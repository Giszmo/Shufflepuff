package com.shuffle.form;

/**
 * Created by Daniel Krawisz on 12/7/15.
 */
class ValueException extends Exception {
    public static enum Values{τ, phase, sender}

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
