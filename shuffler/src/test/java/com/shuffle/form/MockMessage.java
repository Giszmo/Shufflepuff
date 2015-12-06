package com.shuffle.form;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Daniel Krawisz on 12/6/15.
 */
public class MockMessage implements Message {
    Queue<Integer> values;

    MockMessage(int value) {
        this.values = new LinkedList<>();

        this.values.add(value);
    }

    MockMessage(int[] values) {
        this.values = new LinkedList<>();

        for(int val : values) this.values.add(val);
    }

    // These next functions should not be called by this particular test implementation.
    @Override
    public EncryptionKey readAsEncryptionKey() throws FormatException, InvalidImplementationException {
        throw new InvalidImplementationException();
    }

    @Override
    public CoinSignature readAsSignature() throws FormatException, InvalidImplementationException {
        throw new InvalidImplementationException();
    }

    @Override
    public Queue<VerificationKey> readAsVerificationKeyList() throws FormatException, InvalidImplementationException {
        throw new InvalidImplementationException();
    }

    @Override
    public VerificationKey from() throws InvalidImplementationException {
        throw new InvalidImplementationException();
    }

    @Override
    public Message append(Message a) throws InvalidImplementationException, FormatException {
        if (!(a instanceof MockMessage)) {
            throw new InvalidImplementationException();
        }

        values.addAll(((MockMessage)(a)).values);

        return this;
    }

    @Override
    public Message remove() {
        return new MockMessage(values.remove());
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public boolean equal(Message m) throws InvalidImplementationException {
        if (!(m instanceof MockMessage)) {
            throw new InvalidImplementationException();
        }

        Queue<Integer> mock = ((MockMessage)m).values;

        if (mock == values) {
            return true;
        }

        if (values.size() != mock.size()) {
            return false;
        }

        Iterator<Integer> i1 = values.iterator();
        Iterator<Integer> i2 = mock.iterator();

        while (i1.hasNext()) {
            if(!i1.next().equals(i2.next())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Iterator<Message> iterator() {
        return null;
    }

    public String toString() {
        return values.toString();
    }
}
