package com.shuffle.form;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Daniel Krawisz on 12/6/15.
 */
public class MockPacket implements Packet {
    MockVerificationKey from;
    MockVerificationKey to;
    Queue<Integer> values;

    MockPacket(int value) {
        this.values = new LinkedList<>();

        this.values.add(value);
    }

    MockPacket(int[] values) {
        this.values = new LinkedList<>();

        for(int val : values) this.values.add(val);
    }

    // These next functions should not be called by this particular test implementation.
    @Override
    public EncryptionKey readEncryptionKey() throws InvalidImplementationException {
        throw new InvalidImplementationException();
    }

    @Override
    public CoinSignature readCoinSignature() throws InvalidImplementationException {
        throw new InvalidImplementationException();
    }

    @Override
    public VerificationKey readVerificationKey() throws InvalidImplementationException {
        throw new InvalidImplementationException();
    }

    @Override
    public Packet append(VerificationKey vk) throws InvalidImplementationException {
        throw new InvalidImplementationException();
    }

    @Override
    public Packet append(EncryptionKey vk) throws InvalidImplementationException {
        throw new InvalidImplementationException();
    }

    @Override
    public Packet append(CoinSignature sig) throws InvalidImplementationException {
        throw new InvalidImplementationException();
    }

    @Override
    public Packet append(ShufflePhase phase) throws InvalidImplementationException {
        throw new InvalidImplementationException();
    }

    @Override
    public Packet append(SessionIdentifier τ) throws InvalidImplementationException {
        throw new InvalidImplementationException();
    }

    @Override
    public Packet append(Packet a) throws InvalidImplementationException {
        if (!(a instanceof MockPacket)) {
            throw new InvalidImplementationException();
        }

        values.addAll(((MockPacket) (a)).values);

        return this;
    }

    @Override
    public Packet poll() {
        Integer value;
        if ((value = values.poll()) != null) {
            return new MockPacket(value);
        }

        return null;
    }


    @Override
    public boolean equal(Packet m) throws InvalidImplementationException {
        if (!(m instanceof MockPacket)) {
            throw new InvalidImplementationException();
        }

        Queue<Integer> mock = ((MockPacket)m).values;

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
    public SessionIdentifier readSessionIdentifier() {
        return null;
    }

    @Override
    public ShufflePhase readShufflePhase() {
        return null;
    }

    public String toString() {
        return values.toString();
    }
}
