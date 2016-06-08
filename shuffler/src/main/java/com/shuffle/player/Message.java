package com.shuffle.player;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.packet.SessionIdentifier;
import com.shuffle.p2p.Bytestring;
import com.shuffle.protocol.FormatException;
import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.blame.Blame;
import com.shuffle.protocol.message.Phase;

import java.io.Serializable;
import java.util.Deque;
import java.util.Iterator;

/**
 * Implementation of coin shuffle messages.
 *
 * Created by Daniel Krawisz on 5/26/16.
 */
public class Message implements com.shuffle.protocol.message.Message, Serializable {

    public static class SecureHash implements Serializable {
        public final Atom hashed;

        public SecureHash(Atom toHash) {
            hashed = toHash;
        }

        public SecureHash(com.shuffle.protocol.message.Message message) {
            if (!(message instanceof Message)) {
                throw new InvalidImplementationError();
            }

            hashed = ((Message) message).atoms;
        }

        public String toString() {
            return "hashed[" + hashed + "]";
        }

        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }

            if (!(o instanceof SecureHash)) {
                return false;
            }

            SecureHash mockHashed = (SecureHash) o;

            return hashed.equals(mockHashed.hashed);
        }
    }

    private static class Atom implements Serializable {
        public final Address addr;
        public final EncryptionKey ek;
        public final Bytestring sig;
        public final SecureHash secureHash;
        public final Blame blame;

        public final Transaction t;
        // Sometimes, we have to send whole packets that we previously received.
        public final com.shuffle.protocol.message.Packet packet;

        public final Atom next;

        private Atom(
                Address addr,
                EncryptionKey ek,
                Bytestring sig,
                SecureHash secureHash,
                Blame blame,
                Transaction t,
                com.shuffle.protocol.message.Packet packet,
                Atom next
        ) {
            // Enforce the correct format.
            format:
            {
                if (addr != null) {
                    if (ek != null || sig != null || secureHash != null || blame != null || t != null) {
                        throw new IllegalArgumentException();
                    }
                    break format;
                }

                if (ek != null) {
                    if (sig != null || secureHash != null || blame != null || t != null) {
                        throw new IllegalArgumentException();
                    }
                    break format;
                }

                if (sig != null) {
                    if (secureHash != null || blame != null || t != null) {
                        throw new IllegalArgumentException();
                    }
                    break format;
                }

                if (secureHash != null) {
                    if (blame != null || t != null) {
                        throw new IllegalArgumentException();
                    }
                    break format;
                }

                if (blame != null) {
                    if (t != null) {
                        throw new IllegalArgumentException();
                    }
                    break format;
                }

                if (t != null) {
                    break format;
                }

                throw new IllegalArgumentException();
            }

            this.addr = addr;
            this.ek = ek;
            this.sig = sig;
            this.secureHash = secureHash;
            this.blame = blame;
            this.t = t;
            this.packet = packet;
            this.next = next;
        }

        public static Atom make(Object o, Atom next) {
            if (o instanceof Address) {
                return new Atom((Address) o, null, null, null, null, null, null, next);
            }
            if (o instanceof EncryptionKey) {
                return new Atom(null, (EncryptionKey) o, null, null, null, null, null, next);
            }
            if (o instanceof Bytestring) {
                return new Atom(null, null, (Bytestring) o, null, null, null, null, next);
            }
            if (o instanceof SecureHash) {
                return new Atom(null, null, null, (SecureHash) o, null, null, null, next);
            }
            if (o instanceof Blame) {
                return new Atom(null, null, null, null, (Blame) o, null, null, next);
            }
            if (o instanceof Transaction) {
                return new Atom(null, null, null, null, null, (Transaction) o, null, next);
            }
            if (o instanceof com.shuffle.protocol.message.Packet) {
                return new Atom(null, null, null, null, null, null, (com.shuffle.protocol.message.Packet) o, next);
            }

            throw new IllegalArgumentException();
        }

        private static Atom make(Object o) {
            return make(o, null);
        }

        private static Atom attach(Atom a, Atom o) {
            if (a == null) {
                return o;
            }

            return new Atom(a.addr, a.ek, a.sig, a.secureHash, a.blame, a.t, a.packet, attach(a.next, o));
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }

            if (!(o instanceof Atom)) {
                return false;
            }

            Atom a = (Atom) o;

            return this == a || a.sig == sig
                    && (a.ek == null && ek == null || ek != null && ek.equals(a.ek))
                    && (a.addr == null && addr == null || addr != null && addr.equals(a.addr))
                    && (a.t == null && t == null || t != null && t.equals(a.t))
                    && (a.packet == null && packet == null
                    || packet != null && packet.equals(a.packet))
                    && (a.blame == null && blame == null || blame != null && blame.equals(a.blame))
                    && (a.secureHash == null && secureHash == null || secureHash != null && secureHash.equals(a.secureHash))
                    && (a.next == null && next == null || next != null && next.equals(a.next));
        }

        @Override
        public int hashCode() {
            int hash = addr == null ? 0 : addr.hashCode();
            hash = hash * 15 + (ek == null ? 0 : ek.hashCode());
            hash = hash * 15 + (sig == null ? 0 : sig.hashCode());
            hash = hash * 15 + (this.secureHash == null ? 0 : this.secureHash.hashCode());
            hash = hash * 15 + (blame == null ? 0 : blame.hashCode());
            hash = hash * 15 + (next == null ? 0 : next.hashCode());
            return hash;
        }

        @Override
        public String toString() {
            String str = "";

            if (addr != null) str += addr.toString();

            if (ek != null) str += ek.toString();

            if (sig != null) str += sig.toString();

            if (secureHash != null) str += secureHash.toString();

            if (t != null) str += t.toString();

            if (packet != null) str += packet.toString();

            if (blame != null) str += blame.toString();

            if (next != null) str += "⊕" + next.toString();

            return str;
        }
    }

    public final SessionIdentifier session;
    public final Atom atoms;
    public final VerificationKey from;

    // If this message can be sent, then this is the network by
    // which it is sent. Otherwise, it's null.
    final transient Messages messages;

    public Message(
            SessionIdentifier session,
            VerificationKey from,
            Messages messages) {

        if (from == null || session == null) throw new NullPointerException();

        atoms = null;
        this.session = session;
        this.from = from;
        this.messages = messages;
    }

    private Message(
            SessionIdentifier session,
            VerificationKey from,
            Atom atom,
            Messages messages) {

        if (session == null || from == null) throw new NullPointerException();

        this.from = from;
        atoms = atom;
        this.session = session;
        this.messages = messages;
    }

    public Message(SessionIdentifier session,
                   VerificationKey from,
                   Deque atoms,
                   Messages messages) {

        if (session == null || from == null || atoms == null) throw new NullPointerException();

        Atom atom = null;

        Iterator i = atoms.descendingIterator();

        while (i.hasNext()) {
            atom = Atom.make(i.next(), atom);
        }

        this.atoms = atom;
        this.session = session;
        this.from = from;
        this.messages = messages;
    }

    @Override
    public boolean isEmpty() {
        return atoms == null;
    }

    public com.shuffle.protocol.message.Message attachAddrs(Deque<Address> addrs) {
        if (addrs == null) throw new NullPointerException();

        Message m = new Message(session, from, addrs, messages);

        return new Message(session, from, Atom.attach(atoms, m.atoms), messages);
    }

    @Override
    public com.shuffle.protocol.message.Message attach(EncryptionKey ek) {
        if (ek == null) throw new NullPointerException();

        return new Message(session, from, Atom.attach(atoms, Atom.make(ek)), messages);
    }

    @Override
    public com.shuffle.protocol.message.Message attach(Address addr) {
        if (addr == null) throw new NullPointerException();

        return new Message(session, from, Atom.attach(atoms, Atom.make(addr)), messages);
    }

    @Override
    public com.shuffle.protocol.message.Message attach(Bytestring sig) {
        if (sig == null) throw new NullPointerException();

        return new Message(session, from, Atom.attach(atoms, Atom.make(sig)), messages);
    }

    @Override
    public com.shuffle.protocol.message.Message attach(Blame blame) {
        if (blame == null) throw new NullPointerException();

        return new Message(session, from, Atom.attach(atoms, Atom.make(blame)), messages);
    }

    public com.shuffle.protocol.message.Message hashed() {

        return new Message(session, from, Atom.make(new SecureHash(this)), messages);
    }

    @Override
    public EncryptionKey readEncryptionKey() throws FormatException {
        if (atoms == null || atoms.ek == null) throw new FormatException();

        return atoms.ek;
    }

    @Override
    public Address readAddress() throws FormatException {
        if (atoms == null || atoms.addr == null) throw new FormatException();

        return atoms.addr;
    }

    @Override
    public Blame readBlame() throws FormatException {
        if (atoms == null || atoms.blame == null) throw new FormatException();

        return atoms.blame;
    }

    @Override
    public Bytestring readSignature() throws FormatException {
        if (atoms == null || atoms.sig == null) throw new FormatException();

        return atoms.sig;
    }

    @Override
    public com.shuffle.protocol.message.Message rest() throws FormatException {

        if (atoms == null) throw new FormatException();

        return new Message(session, from, atoms.next, messages);
    }

    @Override
    public com.shuffle.protocol.message.Packet send(Phase phase, VerificationKey to)
            throws InterruptedException {

        if (messages == null) return null;

        return messages.send(this, phase, to);
    }

    @Override
    public boolean equals(Object o) {

        if (o == null) return false;

        if (!(o instanceof Message)) return false;

        Message mock = (Message) o;

        return session.equals(mock.session)
                && ((atoms == null && mock.atoms == null)
                || (atoms != null && atoms.equals(mock.atoms)));
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash = hash * 15 + session.hashCode();
        hash = hash * 15 + atoms.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        if (atoms == null) return "[]";

        return atoms.toString();
    }
}
