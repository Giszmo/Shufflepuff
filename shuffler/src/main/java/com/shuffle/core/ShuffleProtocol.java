package com.shuffle.core;

import com.shuffle.form.BlockChainException;
import com.shuffle.form.Coin;
import com.shuffle.form.CoinAmount;
import com.shuffle.form.Crypto;
import com.shuffle.form.CryptographyException;
import com.shuffle.form.ErrorState;
import com.shuffle.form.FormatException;
import com.shuffle.form.InvalidImplementationException;
import com.shuffle.form.MempoolException;
import com.shuffle.form.ProtocolAbortedException;
import com.shuffle.form.ProtocolStartedException;
import com.shuffle.form.SessionIdentifier;
import com.shuffle.form.SessionIdentifierException;
import com.shuffle.form.ShuffleMachine;
import com.shuffle.form.ShufflePhase;
import com.shuffle.form.SigningKey;
import com.shuffle.form.TimeoutException;
import com.shuffle.form.VerificationKey;

/**
 * Created by Daniel Krawisz on 12/6/15.
 */
public class ShuffleProtocol {
    ShuffleMachine machine;
    SessionIdentifier τ;

    ShuffleProtocol(
            SessionIdentifier τ,
            VerificationKey[] players,
            Crypto crypto,
            Coin coin,
            Internet internet) {


        machine = new ShuffleMachine(τ, players, new MessageFactory(), crypto, coin, new ShuffleNetwork());
    }

    // the phase can be accessed concurrently in case we want to update
    // the user on how the protocol is going.
    public ShufflePhase currentPhase() {
        return machine.currentPhase();
    }

    public ErrorState run(CoinAmount ν, SigningKey sk) {
        // Here we handle a bunch of lower level errors.
        try {
            machine.run(ν,sk);
        } catch (SessionIdentifierException
                | InvalidImplementationException
                | MempoolException
                | BlockChainException
                | CryptographyException
                | TimeoutException
                | ProtocolAbortedException
                | ProtocolStartedException
                | FormatException e) {

            return new ErrorState(τ, machine.currentPhase(), e);
        }

        return null;
    }
}
