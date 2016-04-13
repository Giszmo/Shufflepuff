package com.shuffle.p2p;

/**
 * Created by Eugene Siegel on 4/12/16.
 */

public class MediatorClientChannel<Identity, Payload> implements Channel<Identity, Payload> {


    public MediatorClientChannel() {

    }

    public Channel<Identity, Payload> getChannel(Session<Identity,{to,from,payload}> session) {

    }

    public Peer<Identity,Payload> getPeer(Identity you) {
        return null;
    }

    public Connection<Identity, Payload> open(final Listener<Identity, Payload> listener) {
        return null;
    }

}
