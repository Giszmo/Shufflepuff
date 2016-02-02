package com.shuffle.player;

import java.net.InetSocketAddress;
import java.net.URL;

/**
 * Identity is either an ip address and port, a url, or either of those plus a string name.
 *
 * Created by Daniel Krawisz on 1/29/16.
 */
public class Identity {
    public final String name;
    public final InetSocketAddress ip;
    public final URL url;

    public Identity(InetSocketAddress ip) {
        if (ip == null) {
            throw new NullPointerException();
        }

        this.ip = ip;
        name = null;
        url = null;
    }

    public Identity(URL url) {
        if (url == null) {
            throw new NullPointerException();
        }

        this.url = url;
        name = null;
        ip = null;
    }

    public Identity(InetSocketAddress ip, String name) {
        if (ip == null || name == null) {
            throw new NullPointerException();
        }

        this.ip = ip;
        this.name = name;
        url = null;
    }

    public Identity(URL url, String name) {
        if (url == null || name == null) {
            throw new NullPointerException();
        }

        this.url = url;
        this.name = name;
        ip = null;
    }
}
