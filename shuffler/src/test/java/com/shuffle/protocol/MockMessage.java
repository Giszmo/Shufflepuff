package com.shuffle.protocol;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.bitcoin.Signature;
import com.shuffle.bitcoin.Transaction;

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
        public Address addr = null;
        public EncryptionKey ek = null;
        public Signature sig = null;
        public Hash hash = null;
        public BlameMatrix.Blame blame;

        public Transaction t;
        // Sometimes, we have blockchain send whole packets that we previously received.
        public Packet packet;

        public Atom(Address addr) {
            this.addr = addr;
        }

        public Atom(EncryptionKey ek) {
            this.ek = ek;
        }

        public Atom(Signature sig) {
            this.sig = sig;
        }

        public Atom(Hash hash) {
            this.hash = hash;
        }

        public Atom(Transaction t) {
            this.t = t;
        }

        public Atom(Packet packet) {
            this.packet = packet;
        }

        public Atom(BlameMatrix.Blame blame) {this.blame = blame;}

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Atom)) {
                return false;
            }

            Atom a = (Atom)o;

            return this == a || a.sig == sig &&
                    ((a.ek == null && ek == null) || (a.ek != null && ek != null && ek.equals(a.ek))) &&
                    ((a.addr == null && addr == null) || (a.addr != null && addr != null && addr.equals(a.addr)))
                     && ((a.t == null && t == null) || (a.t != null && t != null && t.equals(a.t))) &&
                    ((a.packet == null && packet == null) || (a.packet != null && packet != null && packet.equals(a.packet)))
                     && ((a.blame == null && blame == null) || (a.blame != null && blame != null && blame.equals(a.blame)));
        }

        @Override
        public int hashCode() {
            int hash = addr == null ? 0 : addr.hashCode();
            hash = hash * 15 + (ek == null ? 0 : ek.hashCode());
            hash = hash * 15 + (sig == null ? 0 : sig.hashCode());
            hash = hash * 15 + (this.hash == null ? 0 : this.hash.hashCode());
            hash = hash * 15 + (blame == null ? 0 : blame.hashCode());
            return hash;
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

            if (t != null) {
                return t.toString();
            }

            if (packet != null) {
                return packet.toString();
            }

            if (blame != null) {
                return blame.toString();
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

            if (t != null) {
                return new Atom(t);
            }

            if (packet != null) {
                return new Atom(packet);
            }

            if (blame != null) {
                return new Atom(blame);
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

    @Override
    public boolean isEmpty() {
        return atoms.size() == 0;
    }


    public Message attach(Atom atom) {
        if (atom == null) {
            throw new NullPointerException();
        }
        atoms.add(atom);
        return this;
    }

    public Message attach(Queue<Atom> atoms) {
        if (atoms == null) {
            throw new NullPointerException();
        }
        this.atoms.addAll(atoms);
        return this;
    }

    public Message attachAddrs(Queue<Address> addrs) {
        if (addrs== null) {
            throw new NullPointerException();
        }
        for (Address addr : addrs) {
            attach(addr);
        }

        return this;
    }

    @Override
    public Message attach(EncryptionKey ek) {
        if (ek == null) {
            throw new NullPointerException();
        }
        atoms.add(new Atom(ek));
        return this;
    }

    @Override
    public Message attach(Address addr) {
        if (addr == null) {
            throw new NullPointerException();
        }
        atoms.add(new Atom(addr));
        return this;
    }

    @Override
    public Message attach(Signature sig) {
        if (sig == null) {
            throw new NullPointerException();
        }
        atoms.add(new Atom(sig));
        return this;
    }

    @Override
    public Message attach(BlameMatrix.Blame blame) {
        if (blame == null) {
            throw new NullPointerException();
        }
        atoms.add(new Atom(blame));
        return this;
    }

    @Override
    public Message attach(Message message) throws InvalidImplementationError {
        if (message == null) {
            throw new NullPointerException();
        }
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
    public Address readAddress() throws FormatException {
        Atom atom = atoms.peek();
        if (atom == null || atom.addr == null) {
            throw new FormatException();
        }

        return atoms.remove().addr;
    }

    @Override
    public BlameMatrix.Blame readBlame() throws FormatException {
        Atom atom = atoms.peek();
        if (atom == null || atom.blame == null) {
            throw new FormatException();
        }

        return atoms.remove().blame;
    }

    @Override
    public Signature readSignature() throws FormatException {
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
