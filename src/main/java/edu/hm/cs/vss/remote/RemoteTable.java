package edu.hm.cs.vss.remote;

import edu.hm.cs.vss.*;
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
public class RemoteTable extends Observable implements Table {
    private final RmiTable table;
    private final String host;
    private final Logger logger;
    private Backup backup;

    public RemoteTable(final String host, Logger logger) throws Exception {
        this.host = host;
        this.logger = logger;
        final Registry registry = LocateRegistry.getRegistry(host, STATIC_PORT);
        table = (RmiTable) registry.lookup(Table.class.getSimpleName());
    }

    @Override
    public String getName() {
        return host;
    }

    @Override
    public void addTable(final String tableHost) {
        try {
            logger.log("Requesting the remote table " + host + " to add the table " + tableHost);
            backup = table.getBackupDetails();
            table.addTable(tableHost);
        } catch (RemoteException e) {
            logger.log(e.getMessage());
            e.printStackTrace();
            notifyObservers(this);
        }
    }

    @Override
    public void removeTable(final String tableHost) {
        try {
            logger.log("Requesting the remote table " + host + " to delete the table " + tableHost);
            backup = table.getBackupDetails();
            table.removeTable(tableHost);
        } catch (RemoteException e) {
            logger.log(e.getMessage());
            e.printStackTrace();
            notifyObservers(this);
        }
    }

    @Override
    public Stream<Table> getTables() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Philosopher addPhilosopher(Philosopher philosopher) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removePhilosopher(Philosopher philosopher) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Philosopher> getPhilosophers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addChair(Chair chair) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeChair(Chair chair) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Chair> getChairs() {
        try {
            logger.log("Requesting chairs from remote table " + host);
            backup = table.getBackupDetails();
            return IntStream.rangeClosed(0, table.getChairCount() - 1)
                    .mapToObj(index -> {
                        try {
                            return table.getChair(index);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            notifyObservers(this);
                        }
                        return null;
                    })
                    .filter(chair -> chair != null);
        } catch (RemoteException e) {
            logger.log(e.getMessage());
            e.printStackTrace();
            notifyObservers(this);
        }
        return Stream.empty();
    }

    @Override
    public Chair getNeighbourChair(Chair chair) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTableMaster(TableMaster tableMaster) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TableMaster getTableMaster() {
        logger.log("Requesting the remote table " + host + " to check if the philosopher is allowed to take a seat");
        try {
            backup = table.getBackupDetails();
            return table.getMaster();
        } catch (RemoteException e) {
            logger.log(e.getMessage());
            e.printStackTrace();
            notifyObservers(this);
        }
        return (TableMaster) philosopher -> true;
    }

    public Backup getBackup() {
        return backup;
    }
}
