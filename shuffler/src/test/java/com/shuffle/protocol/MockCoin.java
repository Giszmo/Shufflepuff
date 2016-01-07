package com.shuffle.protocol;

import com.shuffle.cryptocoin.Address;
import com.shuffle.cryptocoin.Coin;
import com.shuffle.cryptocoin.CoinNetworkError;
import com.shuffle.cryptocoin.Transaction;
import com.shuffle.cryptocoin.VerificationKey;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 *
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MockCoin implements Coin {
    public static class Output {
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
            if (!(o instanceof Output)) {
               return false;
            }

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
    public static class MockTransaction implements Transaction {
        List<Output> inputs;
        List<Output> outputs;

        public MockTransaction(List<Output> inputs, List<Output> outputs) {
            this.inputs = inputs;
            this.outputs = outputs;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }

            if (!(o instanceof MockTransaction)) {
                return false;
            }

            MockTransaction mock = (MockTransaction)o;

            if (this == mock) {
                return true;
            }

            if (inputs.size() != mock.inputs.size()) {
                return false;
            }

            if (outputs.size() != mock.outputs.size()) {
                return true;
            }

            Iterator<Output> i1 = inputs.iterator();
            Iterator<Output> i2 = mock.inputs.iterator();

            while(i1.hasNext()) {
                Output c1 = i1.next();
                Output c2 = i2.next();

                if (!c1.equals(c2)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public String toString() {
            return "{" + inputs.toString() + " ==> " + outputs.toString() + "}";
        }
    }

    final ConcurrentHashMap<Address, Output> blockchain = new ConcurrentHashMap<>();
    // The transaction that spends an output.
    final ConcurrentHashMap<Output, Transaction> spend = new ConcurrentHashMap<>();
    final ConcurrentHashMap<Output, Transaction> sent = new ConcurrentHashMap<>();

    public MockCoin(Map<Address, Output> blockchain) {
        this.blockchain.putAll(blockchain);
    };

    public MockCoin() {
    }

    public synchronized void put(Address addr, long value) {
        Output entry = new Output(addr, value);
        blockchain.put(addr, entry);
    }

    @Override
    public synchronized void send(Transaction t) {
        if (!(t instanceof MockTransaction)) {
            throw new InvalidImplementationError();
        }

        MockTransaction mt = (MockTransaction) t;

        // First check that the transaction doesn't send more than it spends.
        long available = 0;
        for (Output input : mt.inputs) {
            available += input.amountHeld;
        }

        for (Output output : mt.outputs) {
            available -= output.amountHeld;
        }

        if (available < 0) {
            throw new CoinNetworkError();
        }

        // Does the transaction spend from valid outputs?
        for (Output input : mt.inputs) {
            if (!blockchain.get(input.address).equals(input)) {
                throw new CoinNetworkError();
            }
        }

        for (Output input : mt.inputs) {
            Transaction nt = spend.get(input);

            if (nt == null) {
                continue;
            }

            if (mt.equals(nt)) {
                return;
            } else {
                throw new CoinNetworkError();
            }
        }

        // Register the transaction.
        for (Output input : mt.inputs) {
            spend.put(input, t);
        }

        for (Output output : mt.outputs) {
            blockchain.put(output.address, output);
            sent.put(output, t);
        }
    }

    @Override
    public synchronized long valueHeld(Address addr) {
        Output entry = blockchain.get(addr);
        if (spend.get(entry) != null) {
            return 0;
        }

        return entry.amountHeld;
    }

    @Override
    // TODO transaction fees.
    public Transaction shuffleTransaction(long amount, List<VerificationKey> from, Queue<Address> to, Map<VerificationKey, Address> changeAddresses) {
        List<Output> inputs = new LinkedList<>();
        List<Output> outputs = new LinkedList<>();

        // Are there inputs big enough blockchain make this transaction?
        for (VerificationKey key : from) {
            Address address = key.address();
            long value = valueHeld(address);
            if (value < amount) {
                throw new CoinNetworkError();
            }

            inputs.add(blockchain.get(address));

            // If a change address has been provided, add that.
            Address change = changeAddresses.get(address);
            if (change != null) {
                outputs.add(new Output(change, value - amount));
            }
        }

        for(Address address : to) {
            outputs.add(new Output(address, amount));
        }
        return new MockTransaction(inputs, outputs);
    }

    @Override
    public Transaction getOffendingTransaction(Address addr, long ν) {
        if (valueHeld(addr) >= ν) {
            return null;
        }

        Output output = blockchain.get(addr);
        Transaction t = spend.get(output);

        if (t != null) {
            return t;
        }

        return sent.get(output);
    }

    @Override
    public boolean isOffendingTransaction(Address addr, long ν, Transaction t) {
        return t.equals(getOffendingTransaction(addr, ν));
    }

}
