package com.shuffle.form;

import java.util.Map;
import java.util.Set;

/**
 * The connection to the other players of the protocol.
 *
 * Created by Daniel Krawisz on 12/3/15.
 */
public interface Network {
    void broadcast(Message σ) throws TimeoutException; // Send a message to everyone.
    void sendTo(VerificationKey i, Message σ) throws TimeoutException; // Send to a particular participant.
    Message receive() throws TimeoutException, ProtocolAbortedException; // Wait for a message to be received.
    // Receive a set of messages from a set of other players.
    Map<VerificationKey, Message> receive(Set<VerificationKey> from) throws TimeoutException, ProtocolAbortedException;
    // Regester the session identifier we expect and set of keys from whose owners we expect to receive messages.
    void register(SessionIdentifier τ, Set<VerificationKey> keys);
}
