/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.mock;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.bitcoin.Signature;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.protocol.FormatException;
import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.Message;
import com.shuffle.protocol.Packet;
import com.shuffle.protocol.SignedPacket;
import com.shuffle.protocol.blame.Blame;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.Serializable;
import java.util.Deque;
import java.util.Iterator;

/**
 */
public class MockMessage implements Message, Serializable {
    private static transient Logger log = LogManager.getLogger(MockMessage.class);

    public static class Hash implements Serializable {
        public final Atom hashed;

        public Hash(Atom toHash) {
            hashed = toHash;
        }

        public Hash(Message message) {
            if (!(message instanceof MockMessage)) {
                throw new InvalidImplementationError();
            }

            hashed = ((MockMessage)message).atoms;
        }

        public String toString() {
            return "hashed[" + hashed + "]";
        }

        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }

            if (!(o instanceof Hash)) {
                return false;
            }

            Hash mockHashed = (Hash)o;

            return new MockMessage(hashed).equals(new MockMessage(mockHashed.hashed));
        }
    }

    private static class Atom implements Serializable {
        public final Address addr;
        public final EncryptionKey ek;
        public final Signature sig;
        public final Hash hash;
        public final Blame blame;

        public final Transaction t;
        // Sometimes, we have to send whole packets that we previously received.
        public final Packet packet;

        public final Atom next;

        private Atom(
                Address addr,
                EncryptionKey ek,
                Signature sig,
                Hash hash,
                Blame blame,
                Transaction t,
                Packet packet,
                Atom next
        ) {
            // Enforce the correct format.
            format : {
                if (addr != null) {
                    if (ek != null || sig != null || hash != null || blame != null || t != null) {
                        throw new IllegalArgumentException();
                    }
                    break format;
                }

                if (ek != null) {
                    if (sig != null || hash != null || blame != null || t != null) {
                        throw new IllegalArgumentException();
                    }
                    break format;
                }

                if (sig != null) {
                    if (hash != null || blame != null || t != null) {
                        throw new IllegalArgumentException();
                    }
                    break format;
                }

                if (hash != null) {
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
            this.hash = hash;
            this.blame = blame;
            this.t = t;
            this.packet = packet;
            this.next = next;
        }

        public static Atom make(Object o, Atom next) {
            if (o instanceof Address) {
                return new Atom((Address)o, null, null, null, null, null, null, next);
            }
            if (o instanceof EncryptionKey) {
                return new Atom(null, (EncryptionKey)o, null, null, null, null, null, next);
            }
            if (o instanceof Signature) {
                return new Atom(null, null, (Signature)o, null, null, null, null, next);
            }
            if (o instanceof Hash) {
                return new Atom(null, null, null, (Hash)o, null, null, null, next);
            }
            if (o instanceof Blame) {
                return new Atom(null, null, null, null, (Blame)o, null, null, next);
            }
            if (o instanceof Transaction) {
                return new Atom(null, null, null, null, null, (Transaction)o, null, next);
            }
            if (o instanceof Packet) {
                return new Atom(null, null, null, null, null, null, (Packet)o, next);
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

            return new Atom(a.addr, a.ek, a.sig, a.hash, a.blame, a.t, a.packet, attach(a.next, o));
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }

            if (!(o instanceof Atom)) {
                return false;
            }

            Atom a = (Atom)o;

            return this == a || a.sig == sig
                    && (a.ek == null && ek == null || ek != null && ek.equals(a.ek))
                    && (a.addr == null && addr == null || addr != null && addr.equals(a.addr))
                    && (a.t == null && t == null || t != null && t.equals(a.t))
                    && (a.packet == null && packet == null
                        || packet != null && packet.equals(a.packet))
                    && (a.blame == null && blame == null || blame != null && blame.equals(a.blame))
                    && (a.hash == null && hash == null || hash != null && hash.equals(a.hash))
                    && (a.next == null && next == null || next != null && next.equals(a.next));
        }

        @Override
        public int hashCode() {
            int hash = addr == null ? 0 : addr.hashCode();
            hash = hash * 15 + (ek == null ? 0 : ek.hashCode());
            hash = hash * 15 + (sig == null ? 0 : sig.hashCode());
            hash = hash * 15 + (this.hash == null ? 0 : this.hash.hashCode());
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

            if (hash != null) str += hash.toString();

            if (t != null) str += t.toString();

            if (packet != null) str += packet.toString();

            if (blame != null) str += blame.toString();

            if (next != null) str += "⊕" + next.toString();

            return str;
        }
    }

    final Atom atoms;

    public MockMessage() {
        atoms = null;
    }

    private MockMessage(Atom atom) {
        atoms = atom;
    }

    public MockMessage(Deque atoms) {
        Atom atom = null;

        Iterator i = atoms.descendingIterator();

        while (i.hasNext()) {
            atom = Atom.make(i.next(), atom);
        }

        this.atoms = atom;
    }

    @Override
    public boolean isEmpty() {
        return atoms == null;
    }

    public Message attachAddrs(Deque<Address> addrs) {
        if (addrs == null) throw new NullPointerException();

        MockMessage m = new MockMessage(addrs);

        return new MockMessage(Atom.attach(atoms, m.atoms));
    }

    @Override
    public Message attach(EncryptionKey ek) {
        if (ek == null) throw new NullPointerException();

        return new MockMessage(Atom.attach(atoms, Atom.make(ek)));
    }

    @Override
    public Message attach(Address addr) {
        if (addr == null) throw new NullPointerException();

        return new MockMessage(Atom.attach(atoms, Atom.make(addr)));
    }

    @Override
    public Message attach(Signature sig) {
        if (sig == null) throw new NullPointerException();

        return new MockMessage(Atom.attach(atoms, Atom.make(sig)));
    }

    @Override
    public Message attach(Blame blame) {
        if (blame == null) throw new NullPointerException();

        return new MockMessage(Atom.attach(atoms, Atom.make(blame)));
    }

    public Message attach(Hash hash) {
        if (hash == null) throw new NullPointerException();

        return new MockMessage(Atom.attach(atoms, Atom.make(hash)));
    }

    @Override
    public Message attach(Message message) throws InvalidImplementationError {
        if (message == null) throw new NullPointerException();

        if (!(message instanceof MockMessage)) throw new InvalidImplementationError();

        return new MockMessage(Atom.attach(atoms, ((MockMessage)message).atoms));
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
    public Blame readBlame() throws FormatException, CryptographyError {
        if (atoms == null || atoms.blame == null) throw new FormatException();

        Blame blame = atoms.blame;

        if (blame.packets != null) {
            for (SignedPacket packet : blame.packets) {
                if (!packet.verify()) {
                    log.warn("packet failed verification: " + packet.toString());
                    throw new CryptographyError();
                }
            }
        }

        return blame;
    }

    @Override
    public Signature readSignature() throws FormatException {
        if (atoms == null || atoms.sig == null) throw new FormatException();

        return atoms.sig;
    }

    @Override
    public Message rest() throws FormatException {

        if (atoms == null) throw new FormatException();

        return new MockMessage(atoms.next);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;

        if (!(o instanceof MockMessage)) return false;

        MockMessage mock = (MockMessage)o;

        return (atoms == null && mock.atoms == null) || (atoms != null && atoms.equals(mock.atoms));
    }

    @Override
    public String toString() {
        if (atoms == null) return "";

        return atoms.toString();
    }
}
