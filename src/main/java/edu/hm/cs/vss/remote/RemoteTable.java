package edu.hm.cs.vss.remote;

import edu.hm.cs.vss.*;
import edu.hm.cs.vss.log.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Observable;
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
        this.backupService = BackupService.create(this);
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
            table.addPhilosopher(getLocalHost(), philosopher.getName(), philosopher.isHungry(), philosopher.getMealCount());
        } catch (RemoteException e) {
            handleRemoteTableDisconnected(e);
        }
    }

    @Override
    public void removePhilosopher(Philosopher philosopher) {
        try {
            table.removePhilosopher(getLocalHost(), philosopher.getName());
        } catch (RemoteException e) {
            handleRemoteTableDisconnected(e);
        }
    }

    @Override
    public Stream<Philosopher> getPhilosophers() {
        return getBackupService().getPhilosophers();
    }

    @Override
    public void addChair(Chair chair) {
        try {
            table.addChair(getLocalHost(), chair.toString());
        } catch (RemoteException e) {
            handleRemoteTableDisconnected(e);
        }
    }

    @Override
    public void removeChair(Chair chair) {
        try {
            table.removeChair(getLocalHost(), chair.toString());
        } catch (RemoteException e) {
            handleRemoteTableDisconnected(e);
        }
    }

    @Override
    public Stream<Chair> getChairs() {
        return backupService.getChairs();
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
            table.onStandUp(getLocalHost(), philosopher.getName());
        } catch (RemoteException e) {
            handleRemoteTableDisconnected(e);
        }
    }

    public RmiTable getRmi() {
        return table;
    }

    public void handleRemoteTableDisconnected(final RemoteException e) {
        //logger.log(e.getMessage());
        // e.printStackTrace();
        setChanged();
        notifyObservers(RemoteTable.this);
    }

    private String getLocalHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "127.0.0.1"; // TODO This is a potential bug if the host can not be detected automaticly
        }
    }
}
