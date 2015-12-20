package com.shuffle.protocol;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Daniel Krawisz on 12/19/15.
 */
public class MockMessage implements Message {

    public static class Hash {
        public Queue<Atom> hashed;

        public Hash(Queue<Atom> toHash) {
            hashed = toHash;
        }

        public String toString() {
            return "hashed[" + hashed + "]";
        }

        public Hash copy() {
            Queue<Atom> newAtoms = new LinkedList<>();

            for (Atom atom : hashed) {
                newAtoms.add(atom.copy());
            }

            return new Hash(newAtoms);
        }
    }

    public static class Atom {
        public Coin.CoinAddress addr = null;
        public EncryptionKey ek = null;
        public CoinSignature sig = null;
        public Hash hash = null;

        public Atom(Coin.CoinAddress addr) {
            this.addr = addr;
        }

        public Atom(EncryptionKey ek) {
            this.ek = ek;
        }

        public Atom(CoinSignature sig) {
            this.sig = sig;
        }

        public Atom(Hash hash) {
            this.hash = hash;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Atom)) {
                return false;
            }

            Atom a = (Atom)o;

            return this == a || a.sig == sig &&
                                    ((a.ek == null && ek == null) || (a.ek != null && ek != null && ek.equals(a.ek))) &&
                                    ((a.addr == null && addr == null) || (a.addr != null && addr != null && addr.equals(a.addr)));
        }

        @Override
        public String toString() {

            if (addr != null) {
                return addr.toString();
            }

            if (ek != null) {
                return ek.toString();
            }

            if (sig != null) {
                return sig.toString();
            }

            if (hash != null) {
                return hash.toString();
            }

            return "";
        }

        public Atom copy() {

            if (addr != null) {
                return new Atom(addr);
            }

            if (ek != null) {
                return new Atom(ek);
            }

            if (sig != null) {
                return new Atom(sig);
            }

            if (hash != null) {
                return new Atom(hash.copy());
            }

            return null;
        }
    }

    Queue<Atom> atoms;

    public MockMessage() {
        atoms = new LinkedList<>();
    }

    public MockMessage(Queue<Atom> atoms) {
        this.atoms = atoms;
    }


    public Message attach(Atom atom) {
        atoms.add(atom);
        return this;
    }

    public Message attach(Queue<Atom> atoms) {
        atoms.addAll(atoms);
        return this;
    }

    public Message attachAddrs(Queue<Coin.CoinAddress> addrs) {
        for (Coin.CoinAddress addr : addrs) {
            attach(addr);
        }

        return this;
    }

    @Override
    public boolean isEmpty() {
        return atoms.size() == 0;
    }

    @Override
    public Message attach(EncryptionKey ek) {
        atoms.add(new Atom(ek));
        return this;
    }

    @Override
    public Message attach(Coin.CoinAddress addr) {
        atoms.add(new Atom(addr));
        return this;
    }

    @Override
    public Message attach(CoinSignature sig) {
        atoms.add(new Atom(sig));
        return this;
    }

    @Override
    public Message attach(Message message) throws InvalidImplementationError {
        if (!(message instanceof MockMessage)) {
            throw new InvalidImplementationError();
        }

        atoms.addAll(((MockMessage) message).atoms);
        return this;
    }

    @Override
    public EncryptionKey readEncryptionKey() throws FormatException {
        Atom atom = atoms.peek();
        if (atom == null || atom.ek == null) {
            throw new FormatException();
        }

        atoms.remove();
        return atom.ek;
    }

    @Override
    public Coin.CoinAddress readCoinAddress() throws FormatException {
        Atom atom = atoms.peek();
        if (atom == null || atom.addr == null) {
            throw new FormatException();
        }

        return atoms.remove().addr;
    }

    @Override
    public CoinSignature readCoinSignature() throws FormatException {
        Atom atom = atoms.peek();
        if (atom == null || atom.sig == null) {
            throw new FormatException();
        }

        atoms.remove();
        return atom.sig;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MockMessage)) {
            return false;
        }

        Queue<Atom> mock = ((MockMessage)o).atoms;

        if (mock == atoms) {
            return true;
        }

        if (mock.size() != atoms.size()) {
            return false;
        }

        Iterator<Atom> i1 = atoms.iterator();
        Iterator<Atom> i2 = mock.iterator();

        while (i1.hasNext()) {
            if(!i1.next().equals(i2.next())) {
                return false;
            }
        }

        return true;
    }

    public MockMessage copy() {
        Queue<Atom> newAtoms = new LinkedList<>();

        for (Atom atom : atoms) {
            newAtoms.add(atom.copy());
        }

        return new MockMessage(newAtoms);
    }

    @Override
    public String toString() {
        return atoms.toString();
    }
}
