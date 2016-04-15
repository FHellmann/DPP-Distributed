package edu.hm.cs.vss.remote;

import edu.hm.cs.vss.Chair;
import edu.hm.cs.vss.Fork;

import java.net.Socket;
import java.util.Optional;

/**
 * Created by Fabio Hellmann on 14.04.2016.
 */
public class RemoteChair implements Chair {
    private final Socket socket;

    public RemoteChair(final Socket socket) {
        this.socket = socket;
    }

    @Override
    public Fork getFork() {
        return null;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public Optional<Chair> blockIfAvailable() throws InterruptedException {
        return null;
    }

    @Override
    public void unblock() {

    }

    @Override
    public int getQueueSize() {
        return 0;
    }
}
