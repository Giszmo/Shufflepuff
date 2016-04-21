/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.mock;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Coin;
import com.shuffle.bitcoin.CoinNetworkException;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.InvalidImplementationError;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulation of a cryptocurrency network for testing purposes.
 *
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MockCoin implements com.shuffle.sim.MockCoin {
    public static class Output implements Serializable {
        final Address address;
        final long amountHeld;

        public Output(Address address, long amount) {
            this.address = address;
            this.amountHeld = amount;
        }

        @Override
        public String toString() {
            return "output[" + address.toString() + ", " + amountHeld + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;

            if (!(o instanceof Output)) return false;

            Output out = (Output)o;

            return address.equals(out.address) && amountHeld == out.amountHeld;
        }

        @Override
        public int hashCode() {
            return address.hashCode() + (int)amountHeld;
        }
    }

    /**
     * Created by Daniel Krawisz on 12/8/15.
     */
    public class MockTransaction implements Transaction, Serializable {
        public final List<Output> inputs = new LinkedList<>();
        public final List<Output> outputs = new LinkedList<>();
        // A number used to represented slight variations in a transaction which would
        // result in different signatures being produced.
        public final int z;

        public MockTransaction(List<Output> inputs, List<Output> outputs) {
            this(inputs, outputs, 1);
        }

        public MockTransaction(List<Output> inputs, List<Output> outputs, int z) {
            for (Output output : inputs)
                if (output == null) throw new NullPointerException();

            for (Output output : outputs)
                if (output == null) throw new NullPointerException();

            this.z = z;
            this.inputs.addAll(inputs);
            this.outputs.addAll(outputs);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;

            if (!(o instanceof MockTransaction)) return false;

            MockTransaction mock = (MockTransaction)o;

            if (this == mock) return true;

            if (z != mock.z) return false;

            if (inputs.size() != mock.inputs.size()) return false;

            if (outputs.size() != mock.outputs.size()) return true;

            Iterator<Output> i1 = inputs.iterator();
            Iterator<Output> i2 = mock.inputs.iterator();

            while (i1.hasNext()) {
                Output c1 = i1.next();
                Output c2 = i2.next();

                if (c2 == null) return false;

                if (!c1.equals(c2)) return false;
            }

            return true;
        }

        @Override
        public String toString() {
            return "{" + inputs.toString() + " ==> " + outputs.toString() + "}";
        }

        @Override
        public boolean send() throws CoinNetworkException {
            MockCoin.this.send(this);
            return true;
        }

        public MockTransaction copy() {
            return new MockTransaction(inputs, outputs, z);
        }

        public MockTransaction mutate() {
            return new MockTransaction(inputs, outputs, z + 1);
        }
    }

    final ConcurrentHashMap<Address, Output> blockchain = new ConcurrentHashMap<>();
    // The transaction that spends an output.
    final ConcurrentHashMap<Output, Transaction> spend = new ConcurrentHashMap<>();
    // The transaction that sends to an input.
    final ConcurrentHashMap<Output, Transaction> sent = new ConcurrentHashMap<>();

    public MockCoin(Map<Address, Output> blockchain) {
        this.blockchain.putAll(blockchain);
    }

    public MockCoin() {
    }

    @Override
    public Coin mutated() {
        return new TransactionMutator(this);
    }

    @Override
    public synchronized void put(Address addr, long value) {
        Output entry = new Output(addr, value);
        blockchain.put(addr, entry);
    }

    @Override
    public synchronized Transaction makeSpendingTransaction(Address from, Address to, long amount) {
        Output output = blockchain.get(from);

        if (output == null) return null;

        if (amount > valueHeld(from)) return null;

        List<Output> in = new LinkedList<>();
        List<Output> out = new LinkedList<>();
        in.add(output);
        out.add(new Output(to, amount));

        return new MockTransaction(in, out);
    }

    public synchronized void send(Transaction t) throws CoinNetworkException {
        if (t == null) throw new NullPointerException();

        if (!(t instanceof MockTransaction)) throw new InvalidImplementationError();

        MockTransaction mt = (MockTransaction) t;

        // First check that the transaction doesn't send more than it spends.
        long available = 0;
        for (Output input : mt.inputs) available += input.amountHeld;

        for (Output output : mt.outputs) available -= output.amountHeld;

        if (available < 0) throw new CoinNetworkException(t);

        // Does the transaction spend from valid outputs?
        for (Output input : mt.inputs)
            if (!blockchain.get(input.address).equals(input))
                throw new CoinNetworkException();

        for (Output input : mt.inputs) {
            Transaction nt = spend.get(input);

            if (nt == null) continue;

            if (mt.equals(nt)) return;
            else throw new CoinNetworkException(nt);
        }

        // Register the transaction.
        for (Output input : mt.inputs) spend.put(input, t);

        for (Output output : mt.outputs) {
            blockchain.put(output.address, output);
            sent.put(output, t);
        }
    }

    @Override
    public synchronized long valueHeld(Address addr) {
        Output entry = blockchain.get(addr);
        if (entry == null) return 0;

        if (spend.get(entry) != null) return 0;

        return entry.amountHeld;
    }

    @Override
    // TODO transaction fees.
    public Transaction shuffleTransaction(
            final long amount,
            List<VerificationKey> from,
            Queue<Address> to, Map<VerificationKey,
            Address> changeAddresses) {

        if (amount == 0) throw new IllegalArgumentException();

        List<Output> inputs = new LinkedList<>();
        List<Output> outputs = new LinkedList<>();

        // Are there inputs big enough to make this transaction?
        for (VerificationKey key : from) {
            final Address address = key.address();
            final long value = valueHeld(address);

            Output input = blockchain.get(address);

            if (input == null) return null;

            inputs.add(input);

            // If a change address has been provided, add that.
            Address change = changeAddresses.get(key);
            if (change != null) outputs.add(new Output(change, value - amount));
        }

        for (Address address : to) outputs.add(new Output(address, amount));

        return new MockTransaction(inputs, outputs, 1);
    }

    @Override
    public Transaction getConflictingTransaction(Address addr, long amount) {
        if (valueHeld(addr) >= amount) return null;

        Output output = blockchain.get(addr);

        if (output == null) return null;

        Transaction t = spend.get(output);

        if (t != null) return t;

        return sent.get(output);
    }

    @Override
    public Transaction getSpendingTransaction(Address addr, long amount) {
        Output output = blockchain.get(addr);

        if (output == null) return null;

        return spend.get(output);
    }

    @Override
    public String toString() {
        return "{" + blockchain.values().toString() + ", " + spend.toString() + "}";
    }

    @Override
    public com.shuffle.sim.MockCoin copy() {
        MockCoin newCoin = new MockCoin();

        newCoin.blockchain.putAll(blockchain);
        newCoin.spend.putAll(spend);
        newCoin.sent.putAll(sent);

        return newCoin;
    }
}
