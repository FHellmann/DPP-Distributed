package edu.hm.cs.vss.remote;

import edu.hm.cs.vss.Chair;
import edu.hm.cs.vss.Fork;

import java.rmi.RemoteException;
import java.util.Optional;

/**
 * Created by Fabio Hellmann on 11.05.2016.
 */
public class RemoteChair implements Chair {
    private final Fork fork;
    private final String name;
    private final RemoteTable table;

    public RemoteChair(final Chair chair, final RemoteTable table) {
        this.fork = new RemoteFork(chair.getFork().toString(), table);
        this.name = chair.toString();
        this.table = table;
    }

    @Override
    public Fork getFork() {
        return fork;
    }

    @Override
    public Optional<Chair> blockIfAvailable() throws InterruptedException {
        try {
            return table.getRmi().blockChairIfAvailable(name) ? Optional.of(this) : Optional.empty();
        } catch (RemoteException e) {
            table.handleRemoteTableDisconnected(e);
        }
        return Optional.empty();
    }

    @Override
    public void unblock() {
        try {
            table.getRmi().unblockChair(name);
        } catch (RemoteException e) {
            table.handleRemoteTableDisconnected(e);
        }
    }

    @Override
    public int getWaitingPhilosopherCount() {
        try {
            return table.getRmi().getChairWaitingPhilosophers(name);
        } catch (RemoteException e) {
            table.handleRemoteTableDisconnected(e);
        }
        return 0;
    }

    @Override
    public String toString() {
        return name;
    }
}
