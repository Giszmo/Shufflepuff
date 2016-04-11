/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;

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
            if (x == null) {
                return y.send(message);
            }

            return x.send(message);
        }

        @Override
        public void close() {
            if (x == null) {
                y.close();
                return;
            }

            x.close();
        }

        @Override
        public boolean closed() {
            if (x == null) {
                return y.closed();
            }

            return x.closed();
        }

        @Override
        public Peer<Either<X, Y>, Message> peer() {
            if (x == null) {
                return new EitherPeer(null, y.peer());
            }

            return new EitherPeer(x.peer(), null);
        }
    }

    class EitherPeer extends Either<Peer<X, Message>, Peer<Y, Message>>
            implements Peer<Either<X, Y>, Message> {

        public EitherPeer(Peer<X, Message> x, Peer<Y, Message> y) {
            super(x, y);
        }

        @Override
        public Either<X, Y> identity() {
            if (x == null) {
                return new Either<>(null, y.identity());
            }

            return new Either<>(x.identity(), null);
        }

        @Override
        public Session<Either<X, Y>, Message> openSession(Receiver<Message> receiver)
                throws InterruptedException {

            if (x == null) {
                Session<Y, Message> sy = y.openSession(receiver);

                if (sy == null) return null;

                return new EitherSession(null, sy);
            }

            Session<X, Message> sx = x.openSession(receiver);

            if (sx == null) return null;

            return new EitherSession(sx, null);
        }

        @Override
        public boolean open() {
            if (x == null) {
                return y.open();
            }

            return x.open();
        }

        @Override
        public void close() {
            if (x == null) {
                y.close();
                return;
            }

            x.close();
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
        if (you.x == null) {
            Peer<Y, Message> py = y.getPeer(you.y);

            if (py == null) return null;

            return new EitherPeer(null, py);
        }

        Peer<X, Message> px = x.getPeer(you.x);

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
        public void close() {
            x.close();
            y.close();
        }
    }

    @Override
    public Connection<Either<X, Y>, Message> open(final Listener<Either<X, Y>, Message> listener) {
        Connection<X, Message> cx = x.open(new Listener<X, Message>(){
            @Override
            public Receiver<Message> newSession(Session<X, Message> session) {
                return listener.newSession(new EitherSession(session, null));
            }
        });

        if (cx == null) return null;

        Connection<Y, Message> cy = y.open(new Listener<Y, Message>(){
            @Override
            public Receiver<Message> newSession(Session<Y, Message> session) {
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
