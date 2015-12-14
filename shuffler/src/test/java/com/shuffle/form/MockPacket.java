package com.shuffle.form;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 *
 *
 * Created by Daniel Krawisz on 12/9/15.
 */
public class MockPacket implements Packet{
    public static class Encrypted {
        public EncryptionKey by;
        public Atom encrypted;

        public Encrypted(EncryptionKey by, Atom toEncrypt) {
            this.by = by;
            this.encrypted = toEncrypt;
        }

        public String toString() {
            return "encrypted[" + encrypted.toString() + "]";
        }
    }

    public static class Hashed {
        public Queue<Atom> hashed;

        public Hashed(Queue<Atom> toHash) {
            hashed = toHash;
        }

        public String toString() {
            return "hashed[" + hashed + "]";
        }
    }

    public static class Atom {
        int index = -1;
        public VerificationKey vk = null;
        public EncryptionKey ek = null;
        public CoinSignature sig = null;
        public SessionIdentifier τ = null;
        public ShufflePhase phase = null;
        public Hashed hashed = null;
        public Encrypted encrypted = null;

        public Atom(VerificationKey vk) {
            this.vk = vk;
        }

        public Atom(EncryptionKey ek) {
            this.ek = ek;
        }

        public Atom(CoinSignature sig) {
            this.sig = sig;
        }

        public Atom(SessionIdentifier τ) {
            this.τ = τ;
        }

        public Atom(ShufflePhase phase) {
            this.phase = phase;
        }

        public Atom(int i) {
            index = i;
        }

        public Atom(Hashed hashed) {
            this.hashed = hashed;
        }

        public Atom(Encrypted encrypted) {
            this.encrypted = encrypted;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Atom)) {
                return false;
            }

            Atom a = (Atom)o;

            return a != null &&
                (this == a ||
                    a.τ == τ && a.phase == phase && a.sig == sig && a.index == index &&
                        ((a.ek == null && ek == null) || (a.ek != null && ek != null && ek.equals(a.ek))) &&
                        ((a.vk == null && vk == null) || (a.vk != null && vk != null && vk.equals(a.vk))));
        }

        public String toString() {
            if (index != -1) {
                return String.valueOf(index);
            }

            if (vk != null) {
                return vk.toString();
            }

            if (ek != null) {
                return ek.toString();
            }

            if (sig != null) {
                return sig.toString();
            }

            if (phase != null) {
                return phase.toString();
            }

            if (hashed != null) {
                return hashed.toString();
            }

            if (encrypted != null) {
                return encrypted.toString();
            }

            return "";
        }
    }

    VerificationKey signer;
    Queue<Atom> atoms;

    public MockPacket(VerificationKey signer) {
        this.signer = signer;
        atoms = new LinkedList<>();
    }

    public MockPacket(int[] ints, VerificationKey signer) {
        this.signer = signer;
        atoms = new LinkedList<>();

        for (int i : ints) {
            atoms.add(new Atom(i));
        }
    }

    public MockPacket(int i, VerificationKey signer) {
        this.signer = signer;
        atoms = new LinkedList<>();

        atoms.add(new Atom(i));
    }

    MockPacket(Atom atom, VerificationKey signer) {
        this.signer = signer;
        atoms = new LinkedList<>();
        atoms.add(atom);
    }


    MockPacket(Queue<Atom> atoms, VerificationKey signer) {
        this.signer = signer;
        this.atoms = atoms;
    }

    @Override
    public Packet append(VerificationKey vk) throws InvalidImplementationException {
        atoms.add(new Atom(vk));
        return this;
    }

    @Override
    public Packet append(EncryptionKey ek) throws InvalidImplementationException {
        atoms.add(new Atom(ek));
        return this;
    }

    @Override
    public Packet append(CoinSignature sig) throws InvalidImplementationException {
        atoms.add(new Atom(sig));
        return this;
    }

    @Override
    public Packet append(ShufflePhase phase) throws InvalidImplementationException {
        atoms.add(new Atom(phase));
        return this;
    }

    @Override
    public Packet append(SessionIdentifier τ) throws InvalidImplementationException {
        atoms.add(new Atom(τ));
        return this;
    }

    @Override
    public Packet append(Packet packet) throws InvalidImplementationException, FormatException {
        if (!(packet instanceof MockPacket)) {
            throw new InvalidImplementationException();
        }

        MockPacket mock = (MockPacket)packet;

        atoms.addAll(mock.atoms);

        mock.atoms.clear();

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MockPacket)) {
            return false;
        }

        Queue<Atom> mock = ((MockPacket)o).atoms;

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

    @Override
    public SessionIdentifier removeSessionIdentifier() throws FormatException {
        Atom atom = atoms.peek();
        if (atom != null && atom.τ != null) {
            return atom.τ;
        }
        throw new FormatException();
    }

    @Override
    public ShufflePhase removeShufflePhase() throws FormatException {
        Atom atom = atoms.peek();
        if (atom != null && atom.phase != null) {
            return atom.phase;
        }
        throw new FormatException();
    }

    @Override
    public EncryptionKey removeEncryptionKey() throws FormatException, InvalidImplementationException {
        Atom atom = atoms.peek();
        if (atom != null && atom.ek != null) {
            return atom.ek;
        }
        throw new FormatException();
    }

    @Override
    public VerificationKey removeVerificationKey() throws FormatException, InvalidImplementationException {
        Atom atom = atoms.peek();
        if (atom != null && atom.vk != null) {
            return atom.vk;
        }
        throw new FormatException();
    }

    @Override
    public CoinSignature removeCoinSignature() throws FormatException, InvalidImplementationException {
        Atom atom = atoms.peek();
        if (atom != null && atom.sig != null) {
            return atom.sig;
        }
        throw new FormatException();
    }

    @Override
    public Packet poll() {
        if (atoms.isEmpty()) {
            return null;
        }

        return new MockPacket(atoms.poll(), signer);
    }

    public String toString() {
        return atoms.toString();
    }
}
