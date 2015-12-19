package com.shuffle.protocol;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 *
 *
 * Created by Daniel Krawisz on 12/9/15.
 */
public class MockMessage implements Message {
    public static class Encrypted {
        public EncryptionKey by;
        public Atom encrypted;

        public Encrypted(EncryptionKey by, Atom toEncrypt) {
            this.by = by;
            this.encrypted = toEncrypt;
        }

        public String toString() {
            return "encrypted[" + encrypted.toString() + " by " + by.toString() + "]";
        }
    }

    public static class Hash {
        public Queue<Atom> hashed;

        public Hash(Queue<Atom> toHash) {
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
        public Hash hash = null;
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

        public Atom(int i) {
            index = i;
        }

        public Atom(Hash hash) {
            this.hash = hash;
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
                    /*a.τ == τ && a.phase == phase && */a.sig == sig && a.index == index &&
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

            if (hash != null) {
                return hash.toString();
            }

            if (encrypted != null) {
                return encrypted.toString();
            }

            return "";
        }
    }

    Queue<Atom> atoms;
    SessionIdentifier τ;
    ShufflePhase phase;
    SigningKey signer;

    public MockMessage(SessionIdentifier τ, ShufflePhase phase, SigningKey signer) {
        if (τ == null || phase == null || signer == null) {
            throw new NullPointerException();
        }

        this.signer = signer;
        this.phase = phase;
        this.τ = τ;
        atoms = new LinkedList<>();
    }

    public MockMessage attach(int i) {
        atoms.add(new Atom(i));
        return this;
    }

    public MockMessage attach(int[] ints) {
        for (int i : ints) {
            atoms.add(new Atom(i));
        }
        return this;
    }

    public MockMessage attach(Atom atom) {
        atoms.add(atom);
        return this;
    }

    public MockMessage attach(Queue<Atom> atoms) {
        this.atoms.addAll(atoms);
        return this;
    }

    @Override
    public Message attach(VerificationKey vk) throws InvalidImplementationException {
        atoms.add(new Atom(vk));
        return this;
    }

    @Override
    public Message attach(EncryptionKey ek) throws InvalidImplementationException {
        atoms.add(new Atom(ek));
        return this;
    }

    @Override
    public Message attach(CoinSignature sig) throws InvalidImplementationException {
        atoms.add(new Atom(sig));
        return this;
    }

    @Override
    public Message attach(Message message) throws InvalidImplementationException, FormatException {
        if (!(message instanceof MockMessage)) {
            throw new InvalidImplementationException();
        }

        MockMessage mock = (MockMessage) message;

        atoms.addAll(mock.atoms);

        return this;
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

    @Override
    public SessionIdentifier getSessionIdentifier() throws FormatException {
        return τ;
    }

    @Override
    public ShufflePhase getShufflePhase() throws FormatException {
        return phase;
    }

    @Override
    public VerificationKey getSigner() throws FormatException, CryptographyException {
        return signer.VerificationKey();
    }

    @Override
    public EncryptionKey removeEncryptionKey() throws FormatException, InvalidImplementationException {
        Atom atom = atoms.peek();
        if (atom == null || atom.ek == null) {
            throw new FormatException();
        }

        atoms.remove();
        return atom.ek;
    }

    @Override
    public VerificationKey removeVerificationKey() throws FormatException, InvalidImplementationException {
        Atom atom = atoms.peek();
        if (atom == null || atom.vk == null) {
            throw new FormatException();
        }

        atoms.remove();
        return atom.vk;
    }

    @Override
    public CoinSignature removeCoinSignature() throws FormatException, InvalidImplementationException {
        Atom atom = atoms.peek();
        if (atom == null || atom.sig == null) {
            throw new FormatException();
        }

        atoms.remove();
        return atom.sig;
    }

    @Override
    public Message poll() {
        if (atoms.isEmpty()) {
            return null;
        }

        return new MockMessage(τ, phase, signer).attach(atoms.poll());
    }

    @Override
    public String toString() {
        return "{" + atoms.toString() + ", " + τ.toString() + ", " + phase.toString() + ", " + signer.toString() + "}";
    }
}
