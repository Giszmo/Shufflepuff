package com.shuffle.core;

import com.shuffle.form.EncryptionKey;
import com.shuffle.form.InvalidImplementationException;
import com.shuffle.form.Message;
import com.shuffle.form.SessionIdentifier;
import com.shuffle.form.ShuffleMachine;
import com.shuffle.form.CoinSignature;
import com.shuffle.form.SigningKey;
import com.shuffle.form.VerificationKey;

/**
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MessageFactory implements com.shuffle.form.MessageFactory {
    MessageHeader currentHeader;
    SessionIdentifier τ;
    SigningKey sk;
    ShuffleMachine machine;

    private void resetHeader() {
        if (machine.currentPhase() != currentHeader.phase) {
            currentHeader = new MessageHeader(τ, sk.VerificationKey(), machine.currentPhase());
        }
    }

    @Override
    public Message make() {
        resetHeader();

        return new com.shuffle.core.Message(currentHeader);
    }

    @Override
    public Message make(EncryptionKey ek) {
        resetHeader();
        return new com.shuffle.core.Message(currentHeader, new MessageElement(ek));
    }

    @Override
    public Message make(VerificationKey vk) {
        resetHeader();
        return new com.shuffle.core.Message(currentHeader, new MessageElement(vk));
    }

    @Override
    public Message make(CoinSignature s) {
        resetHeader();
        return new com.shuffle.core.Message(currentHeader, new MessageElement(s));
    }

    @Override
    public Message remake(Message m) throws InvalidImplementationException {
        resetHeader();

        if (!(m instanceof com.shuffle.core.Message)) {
            throw new InvalidImplementationException();
        }

        return new com.shuffle.core.Message(currentHeader, ((com.shuffle.core.Message)m).elements);
    }

    @Override
    public void register(SessionIdentifier τ, SigningKey sk, ShuffleMachine m) {
        this.τ = τ;
        this.sk = sk;
        this.machine = m;
    }

}
