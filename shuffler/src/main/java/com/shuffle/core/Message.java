package com.shuffle.core;

import com.shuffle.form.EncryptionKey;
import com.shuffle.form.FormatException;
import com.shuffle.form.InvalidImplementationException;
import com.shuffle.form.CoinSignature;
import com.shuffle.form.VerificationKey;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Daniel Krawisz on 12/5/15.
 */
public class Message implements com.shuffle.form.Message
{
    Queue<MessageElement> elements;
    MessageHeader header;
    ElementType type;

    public Message(MessageHeader header) {
        this.type = ElementType.Unset;
        this.header = header;
        this.elements = new LinkedList<>();
    }

    public Message(MessageHeader header, Queue<MessageElement> elements) {
        this.type = elements.peek().type();
        this.header = header;
        this.elements = elements;
    }

    public Message(MessageHeader header, MessageElement element) {
        this.type = element.type();
        this.header = header;
        elements = new LinkedList<>();
        elements.add(element);
    }

    @Override
    public EncryptionKey readAsEncryptionKey() throws FormatException {
        if (elements.size() != 1) {
            throw new FormatException();
        }

        return elements.peek().readAsEncryptionKey();
    }

    @Override
    public CoinSignature readAsSignature() throws FormatException {
        if (elements.size() != 1) {
            throw new FormatException();
        }

        return elements.peek().readAsSignature();
    }

    @Override
    //TODO
    public Queue<VerificationKey> readAsVerificationKeyList() throws FormatException {
        return null;
    }

    @Override
    public VerificationKey from() {
        return header.from;
    }

    @Override
    public com.shuffle.form.Message append(com.shuffle.form.Message a) throws InvalidImplementationException, FormatException {
        if (!(a instanceof Message)) {
            throw new InvalidImplementationException();
        }

        Message b = (Message)a;

        if (header != b.header) {
            throw new InvalidImplementationException();
        }

        if (type != b.type) {
            throw new FormatException();
        }

        elements.addAll(b.elements);
        return this;
    }

    @Override
    public com.shuffle.form.Message remove() {
        return new Message(header, elements.remove());
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public boolean equal(com.shuffle.form.Message m) {
        if (m.size() != elements.size()) {
            return false;
        }

        Iterator<MessageElement> it = elements.iterator();
        for(com.shuffle.form.Message sub : m) {
            if (!it.next().equals(sub)) {
                return false;
            }
        }

        return true;
    }

    @Override
    // TODO
    public Iterator<com.shuffle.form.Message> iterator() {
        return null;
    }
}
