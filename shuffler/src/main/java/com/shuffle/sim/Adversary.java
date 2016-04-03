/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.sim;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.CoinNetworkException;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.BasicChan;
import com.shuffle.chan.Chan;
import com.shuffle.monad.Either;
import com.shuffle.monad.Summable;
import com.shuffle.monad.SummableMap;
import com.shuffle.monad.SummableMaps;
import com.shuffle.protocol.CoinShuffle;
import com.shuffle.protocol.FormatException;
import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.InvalidParticipantSetException;
import com.shuffle.protocol.Network;
import com.shuffle.protocol.SessionIdentifier;
import com.shuffle.protocol.SignatureException;
import com.shuffle.protocol.ValueException;
import com.shuffle.protocol.blame.Matrix;

import java.net.ProtocolException;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

    // Run the protocol in a separate thread and get a future to the final state.
    private static Future<Summable.SummableElement<Map<SigningKey,
            Either<Transaction, Matrix>>>> runProtocolFuture(

            final CoinShuffle shuffle,
            final SessionIdentifier session, // Unique session identifier.
            final long amount, // The amount to be shuffled per player.
            final SigningKey sk, // The signing key of the current player.
            // The set of players, sorted alphabetically by address.
            final SortedSet<VerificationKey> players,
            final Address change, // Change address. (can be null)
            final Network network // The network that connects us to the other players.
    ) {
        final Chan<Either<Transaction, Matrix>> q = new BasicChan<>();

        if (amount <= 0) {
            throw new IllegalArgumentException();
        }
        if (session == null || sk == null || players == null || network == null) {
            throw new NullPointerException();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    try {
                        q.send(new Either<Transaction, Matrix>(shuffle.runProtocol(
                                session, amount, sk, players, change, network, null
                        ), null));

                    } catch (Matrix m) {
                        q.send(new Either<Transaction, Matrix>(null, m));
                    }

                } catch (InterruptedException
                        | ProtocolException
                        | SignatureException
                        | com.shuffle.protocol.TimeoutException
                        | ValueException
                        | FormatException
                        | CoinNetworkException
                        | InvalidParticipantSetException e) {

                    e.printStackTrace();
                    System.out.println("Exception returned by " + sk + ": " + e.getMessage());
                }

                q.close();
            }
        }).start();

        return new Future<Summable.SummableElement<Map<SigningKey, Either<Transaction, Matrix>>>>(
        ) {
            Summable.SummableElement<Map<SigningKey, Either<Transaction, Matrix>>> result = null;
            boolean done = false;

            @Override
            public boolean cancel(boolean b) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return done || q.closed();
            }

            private Summable.SummableElement<Map<SigningKey, Either<Transaction, Matrix>>> g(
                    Either<Transaction, Matrix> m
            ) {
                if (m == null) {
                    return new SummableMaps.Zero<>();
                }
                return new SummableMap<>(sk, m);
            }

            @Override
            public synchronized Summable.SummableElement<Map<SigningKey,
                    Either<Transaction, Matrix>>> get()
                    throws InterruptedException, ExecutionException {
                if (done) {
                    return result;
                }

                result = g(q.receive());
                done = true;
                return result;
            }

            @Override
            public synchronized Summable.SummableElement<Map<SigningKey,
                    Either<Transaction, Matrix>>> get(long l, TimeUnit timeUnit)
                    throws InterruptedException, ExecutionException,
                    java.util.concurrent.TimeoutException {

                if (done) {
                    return result;
                }

                Either<Transaction, Matrix> r = q.receive(l, timeUnit);

                if (r != null) {
                    result = g(r);
                    done = true;
                    return result;
                }

                if (q.closed()) {
                    done = true;
                }

                return null;
            }
        };
    }

    public SessionIdentifier session() {
        return session;
    }

    // Return a future that can be composed with others.
    public Future<Summable.SummableElement<Map<SigningKey, Either<Transaction, Matrix>>>> turnOn(
    ) throws InvalidImplementationError {

        return runProtocolFuture(shuffle, session, amount, sk, players, null, network);
    }

    public SigningKey identity() {
        return sk;
    }
}
