package com.shuffle.p2p;

/**
 * Created by Eugene Siegel on 4/12/16.
 */

public class MediatorClientChannel<Name, Address, Payload> implements Channel<Name, Payload> {

    public class Envelope {
        private Name to;
        private Name from;
        private Payload payload;

        public void setTo(Name to) {
            this.to = to;
        }

        public void setFrom(Name from) {
            this.from = from;
        }

        public void setPayload(Payload payload) {
            this.payload = payload;
        }
    }

    public MediatorClientChannel(final Session<Address, Envelope> session) {


        Channel<Name, Payload> c1 = new Channel<Name, Payload>() {
            @Override
            public Peer<Name, Payload> getPeer(Name you) {
                return session.peer(); // incompatible types
                //return null;
            }

            @Override
            public Connection<Name, Payload> open(Listener<Name, Payload> listener) {
                return null;
            }
        };
        // return Channel<Name,Payload>
    }

    /*
    public Channel<Identity, Payload> getChannel(Session<Identity,Payload> session) {
        return null;
    }*/

    public Peer<Name,Payload> getPeer(Name you) {
        return null;
    }

    public Connection<Name, Payload> open(final Listener<Name, Payload> listener) {
        return null;
    }

}
