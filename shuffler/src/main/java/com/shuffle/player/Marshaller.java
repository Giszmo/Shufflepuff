package com.shuffle.player;

import com.shuffle.p2p.Bytestring;
import com.shuffle.protocol.FormatException;
import com.shuffle.protocol.SignedPacket;

import java.io.IOException;

/**
 * Created by Daniel Krawisz on 1/31/16.
 */
public interface Marshaller<Format> {
    public Format marshall(SignedPacket packet);
    public SignedPacket unmarshall(Format string) throws FormatException;
}
