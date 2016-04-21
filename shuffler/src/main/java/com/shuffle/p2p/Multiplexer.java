/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;

import com.shuffle.chan.Send;
import com.shuffle.monad.Either;

/**
 * A channel that is the product of two channels.
 *
 * Created by Daniel Krawisz on 1/31/16.
 */
public class Multiplexer<X, Y, Message> implements Channel<Either<X, Y>, Message> {

    class EitherSession extends Either<Session<X, Message>, Session<Y, Message>>
            implements Session<Either<X, Y>, Message> {

        public EitherSession(Session<X, Message> x, Session<Y, Message> y) {
            super(x, y);
        }

        @Override
        public boolean send(Message message) throws InterruptedException {
            if (first == null) {
                return second.send(message);
            }

            return first.send(message);
        }

        @Override
        public void close() throws InterruptedException {
            if (first == null) {
                second.close();
                return;
            }

            first.close();
        }

        @Override
        public boolean closed() throws InterruptedException {
            if (first == null) {
                return second.closed();
            }

            return first.closed();
        }

        @Override
        public Peer<Either<X, Y>, Message> peer() {
            if (first == null) {
                return new EitherPeer(null, second.peer());
            }

            return new EitherPeer(first.peer(), null);
        }
    }

    class EitherPeer extends Either<Peer<X, Message>, Peer<Y, Message>>
            implements Peer<Either<X, Y>, Message> {

        public EitherPeer(Peer<X, Message> x, Peer<Y, Message> y) {
            super(x, y);
        }

        @Override
        public Either<X, Y> identity() {
            if (first == null) {
                return new Either<>(null, second.identity());
            }

            return new Either<>(first.identity(), null);
        }

        @Override
        public Session<Either<X, Y>, Message> openSession(Send<Message> receiver)
                throws InterruptedException {

            if (first == null) {
                Session<Y, Message> sy = second.openSession(receiver);

                if (sy == null) return null;

                return new EitherSession(null, sy);
            }

            Session<X, Message> sx = first.openSession(receiver);

            if (sx == null) return null;

            return new EitherSession(sx, null);
        }

        @Override
        public boolean open() throws InterruptedException {
            if (first == null) {
                return second.open();
            }

            return first.open();
        }

        @Override
        public void close() throws InterruptedException {
            if (first == null) {
                second.close();
                return;
            }

            first.close();
        }
    }

    private final Channel<X, Message> x;
    private final Channel<Y, Message> y;

    public Multiplexer(Channel<X, Message> x, Channel<Y, Message> y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public Peer<Either<X, Y>, Message> getPeer(Either<X, Y> you) {
        if (you.first == null) {
            Peer<Y, Message> py = y.getPeer(you.second);

            if (py == null) return null;

            return new EitherPeer(null, py);
        }

        Peer<X, Message> px = x.getPeer(you.first);

        if (px == null) return null;

        return new EitherPeer(px, null);
    }

    class MultiplexerConnection implements Connection<Either<X, Y>, Message> {
        final Connection<X, Message> x;
        final Connection<Y, Message> y;

        MultiplexerConnection(Connection<X, Message> x, Connection<Y, Message> y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public Either<X, Y> identity() {
            return new Either<X, Y>(x.identity(), y.identity());
        }

        @Override
        public void close() throws InterruptedException {
            x.close();
            y.close();
        }
    }

    @Override
    public Connection<Either<X, Y>, Message> open(final Listener<Either<X, Y>, Message> listener)
            throws InterruptedException {
        
        Connection<X, Message> cx = x.open(new Listener<X, Message>(){
            @Override
            public Send<Message> newSession(Session<X, Message> session) throws InterruptedException {
                return listener.newSession(new EitherSession(session, null));
            }
        });

        if (cx == null) return null;

        Connection<Y, Message> cy = y.open(new Listener<Y, Message>(){
            @Override
            public Send<Message> newSession(Session<Y, Message> session) throws InterruptedException {
                return listener.newSession(new EitherSession(null, session));
            }
        });

        if (cy == null) {
            cx.close();
            return null;
        }

        return new MultiplexerConnection(cx, cy);
    }
}
