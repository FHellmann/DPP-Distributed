package edu.hm.cs.vss.remote;

import edu.hm.cs.vss.Fork;

import java.rmi.RemoteException;
import java.util.Optional;

/**
 * Created by Fabio Hellmann on 11.05.2016.
 */
public class RemoteFork implements Fork {
    private final String name;
    private final RemoteTable table;

    public RemoteFork(final String name, final RemoteTable table) {
        this.name = name;
        this.table = table;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Optional<Fork> blockIfAvailable() {
        try {
            return table.getRmi().blockForkIfAvailable(name) ? Optional.of(this) : Optional.empty();
        } catch (RemoteException e) {
            table.handleRemoteTableDisconnected(e);
        }
        return Optional.empty();
    }

    @Override
    public void unblock() {
        try {
            table.getRmi().unblockFork(name);
        } catch (RemoteException e) {
            table.handleRemoteTableDisconnected(e);
        }
    }
}
