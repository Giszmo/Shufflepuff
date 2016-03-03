package com.shuffle.sim;

import com.shuffle.bitcoin.Coin;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.monad.Summable;
import com.shuffle.monad.SummableMap;
import com.shuffle.protocol.CoinShuffle;
import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.Machine;
import com.shuffle.protocol.Network;
import com.shuffle.protocol.Phase;
import com.shuffle.protocol.SessionIdentifier;
import com.shuffle.protocol.SignedPacket;

import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The adversary provides for an environment in which the protocol can run in a separate thread.
 * Messages can be delivered to it as it runs.
 *
 * Created by Daniel Krawisz on 2/8/16.
 */
public class Adversary {

    private final SessionIdentifier session;
    private final CoinShuffle shuffle;
    private final Network network;
    private final SigningKey sk;
    private final SortedSet<VerificationKey> players;
    private final long amount;

    Adversary(
            SessionIdentifier session,
            long amount,
            SigningKey sk,
            SortedSet<VerificationKey> players,
            CoinShuffle shuffle,
            Network network) {
        this.session = session;
        this.amount = amount;
        this.sk = sk;
        this.network = network;
        this.players = players;
        this.shuffle = shuffle;
    }

    public SessionIdentifier session() {
        return session;
    }

    // Return a future that can be composed with others.
    public Future<Summable.SummableElement<Map<SigningKey, Machine>>> turnOn() throws InvalidImplementationError {
        return new Future<Summable.SummableElement<Map<SigningKey, Machine>>>() {
            final Future<Machine> m = shuffle.runProtocolFuture(session, amount, sk, players, null, network);

            @Override
            public boolean cancel(boolean b) {
                return m.cancel(b);
            }

            @Override
            public boolean isCancelled() {
                return m.isCancelled();
            }

            @Override
            public boolean isDone() {
                return m.isDone();
            }

            private Summable.SummableElement<Map<SigningKey, Machine>> g(Machine m) {
                if (m == null) {
                    return null;
                }
                return new SummableMap<SigningKey, Machine>(sk, m);
            }

            @Override
            public Summable.SummableElement<Map<SigningKey, Machine>> get() throws InterruptedException, ExecutionException {
                return g(m.get());
            }

            @Override
            public Summable.SummableElement<Map<SigningKey, Machine>> get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
                return g(m.get(l, timeUnit));
            }

        };
    }

    public SigningKey identity() {
        return sk;
    }
}
