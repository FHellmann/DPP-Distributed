package edu.hm.cs.vss.remote;

import edu.hm.cs.vss.*;
import edu.hm.cs.vss.local.LocalTableMaster;
import edu.hm.cs.vss.log.Logger;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Observable;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by Fabio Hellmann on 14.04.2016.
 */
public class RemoteTable extends Observable implements Table, Philosopher.OnStandUpListener {
    private final String host;
    private final Logger logger;
    private final RmiTable table;
    private final BackupService backupService;

    public RemoteTable(final String host, Logger logger) throws Exception {
        this.host = host;
        this.logger = logger;
        this.backupService = BackupService.create();
        final Registry registry = LocateRegistry.getRegistry(host, NETWORK_PORT);
        table = (RmiTable) registry.lookup(Table.class.getSimpleName());
    }

    @Override
    public String getName() {
        return host;
    }

    @Override
    public void connectToTable(final String tableHost) {
        try {
            logger.log("Requesting the remote table " + host + " to add the table " + tableHost);
            table.addTable(tableHost);
        } catch (RemoteException e) {
            handleRemoteTableDisconnected(e);
        }
    }

    @Override
    public void disconnectFromTable(final String tableHost) {
        try {
            logger.log("Requesting the remote table " + host + " to delete the table " + tableHost);
            table.removeTable(tableHost);
        } catch (RemoteException e) {
            handleRemoteTableDisconnected(e);
        }
    }

    @Override
    public Stream<Table> getTables() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addPhilosopher(Philosopher philosopher) {
        try {
            table.addPhilosopher(getName(), philosopher.getName(), philosopher.isHungry());
        } catch (RemoteException e) {
            handleRemoteTableDisconnected(e);
        }
    }

    @Override
    public void removePhilosopher(Philosopher philosopher) {
        try {
            table.removePhilosopher(getName(), philosopher.getName());
        } catch (RemoteException e) {
            handleRemoteTableDisconnected(e);
        }
    }

    @Override
    public Stream<Philosopher> getPhilosophers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addChair(Chair chair) {
        try {
            table.addChair(getName(), chair.toString());
        } catch (RemoteException e) {
            handleRemoteTableDisconnected(e);
        }
    }

    @Override
    public void removeChair(Chair chair) {
        try {
            table.removeChair(getName(), chair.toString());
        } catch (RemoteException e) {
            handleRemoteTableDisconnected(e);
        }
    }

    @Override
    public Stream<Chair> getChairs() {
        try {
            //logger.log("Requesting chairs from remote table " + host);
            return IntStream.rangeClosed(0, table.getChairCount() - 1)
                    .mapToObj(index -> {
                        try {
                            return table.getChair(index);
                        } catch (RemoteException e) {
                            handleRemoteTableDisconnected(e);
                        }
                        return null;
                    })
                    .filter(chair -> chair != null);
        } catch (RemoteException e) {
            handleRemoteTableDisconnected(e);
        }
        return Stream.empty();
    }

    @Override
    public Chair getNeighbourChair(Chair chair) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TableMaster getTableMaster() {
        return mealCount -> mealCount <= getBackupService().getPhilosophers()
                .mapToInt(Philosopher::getMealCount).min().orElse(0) + TableMaster.MAX_DEVIATION;
    }

    @Override
    public BackupService getBackupService() {
        return backupService;
    }

    @Override
    public void setTableMaster(TableMaster tableMaster) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onStandUp(Philosopher philosopher) {
        try {
            table.onStandUp(getName(), philosopher.getName());
        } catch (RemoteException e) {
            handleRemoteTableDisconnected(e);
        }
    }

    private void handleRemoteTableDisconnected(final RemoteException e) {
        //logger.log(e.getMessage());
        // e.printStackTrace();
        setChanged();
        notifyObservers(RemoteTable.this);
    }
}
