package com.shuffle.core;

import com.shuffle.form.EncryptionKey;
import com.shuffle.form.FormatException;
import com.shuffle.form.InvalidImplementationException;
import com.shuffle.form.CoinSignature;
import com.shuffle.form.VerificationKey;

/**
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MessageElement {
    EncryptionKey ek = null;
    VerificationKey vk = null;
    CoinSignature sig = null;
    Hash hash = null;
    ElementType type;

    public MessageElement(EncryptionKey ek) {
        this.ek = ek;
        type = ElementType.EncryptionKey;
    }

    public MessageElement(VerificationKey vk) {
        this.vk = vk;
        type = ElementType.VerificationKey;
    }

    public MessageElement(CoinSignature sig) {
        this.sig = sig;
        type = ElementType.Signature;
    }

    boolean equals(MessageElement m) throws InvalidImplementationException {
        if (ek != null) {
            if (m.ek != null) {
                return ek == m.ek || ek.equals(m.ek);
            }
        }
        if (vk != null) {
            if (m.vk != null) {
                return vk == m.vk || vk.equals(m.vk);
            }
        }
        if (sig != null) {
            if (m.sig != null) {
                return sig == m.sig || sig.equals(m.sig);
            }
        }
        if (hash != null) {
            if (m.hash != null) {
                return hash == m.hash || hash.equals(m.hash);
            }
        }
        throw new InvalidImplementationException();
    }

    EncryptionKey readAsEncryptionKey() throws FormatException {
        if (ek == null) {
            throw new FormatException();
        }

        return ek;
    }

    CoinSignature readAsSignature() throws FormatException {
        if (sig == null) {
            throw new FormatException();
        }

        return sig;
    }

    ElementType type() {
        return type;
    }
}
