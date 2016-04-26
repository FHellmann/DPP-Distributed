package edu.hm.cs.vss.local;

import edu.hm.cs.vss.*;
import edu.hm.cs.vss.log.DummyLogger;
import edu.hm.cs.vss.log.Logger;
import edu.hm.cs.vss.remote.RemoteTable;
import edu.hm.cs.vss.remote.RmiTable;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by fhellman on 18.04.2016.
 */
public class LocalTablePool extends UnicastRemoteObject implements RmiTable, Table, Observer {
    private final List<Table> tables = Collections.synchronizedList(new LinkedList<>());
    private final List<Philosopher> localPhilosophers = Collections.synchronizedList(new ArrayList<>());
    private final Table localTable;
    private final Logger logger;

    public LocalTablePool() throws IOException {
        this(new DummyLogger());
    }

    public LocalTablePool(final Logger logger) throws IOException {
        super(NETWORK_PORT);
        this.localTable = new LocalTable(logger);
        this.logger = logger;
        final Registry registry = LocateRegistry.createRegistry(NETWORK_PORT);
        registry.rebind(Table.class.getSimpleName(), this);
        tables.add(this);
    }

    @Override
    public void connectToTable(final String tableHost) {
        logger.log("Try to connect to remote table " + tableHost + "...");
        if (tables.parallelStream().noneMatch(table -> table.getName().equals(tableHost))) {
            try {
                final RemoteTable table = new RemoteTable(tableHost, logger);
                table.addObserver(this);
                tables.add(table);
                tables.stream()
                        .map(Table::getName)
                        .filter(name -> !name.equals(tableHost)) // Skip out the previous added table
                        .forEach(table::connectToTable);
                logger.log("Connected to remote table " + tableHost + "!");
            } catch (Exception e) {
                logger.log(e.getMessage());
                e.printStackTrace();
            }
        } else {
            logger.log("Already connected to " + tableHost + "!");
        }
    }

    @Override
    public void disconnectFromTable(final String tableHost) {
        logger.log("Try to disconnect from remote table " + tableHost + "...");
        tables.parallelStream()
                .skip(1) // Skip the local table
                .peek(table -> table.disconnectFromTable(tableHost))
                .filter(table -> table.getName().equals(tableHost))
                .findAny()
                .ifPresent(host -> {
                    tables.remove(host);
                    logger.log("Disconnected from remote table " + tableHost + "!");
                });
    }

    @Override
    public Stream<Table> getTables() {
        return tables.stream();
    }

    @Override
    public Philosopher addPhilosopher(final Philosopher philosopher) {
        localPhilosophers.add(philosopher);
        philosopher.start();

        // Inform other tables
        try {
            addPhilosopher(getName(), philosopher.getName(), philosopher.isHungry());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return philosopher;
    }

    @Override
    public void removePhilosopher(Philosopher philosopher) {
        philosopher.interrupt();
        localPhilosophers.remove(philosopher);

        // Inform other tables
        try {
            removePhilosopher(getName(), philosopher.getName());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Stream<Philosopher> getPhilosophers() {
        return localPhilosophers.stream();
    }

    @Override
    public void addChair(Chair chair) {
        getLocalTable().addChair(chair);

        // Inform other tables
        try {
            addChair(getName(), chair.toString());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeChair(Chair chair) {
        getLocalTable().removeChair(chair);

        // Inform other tables
        try {
            removeChair(getName(), chair.toString());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Stream<Chair> getChairs() {
        return getLocalTable().getChairs();
    }

    @Override
    public TableMaster getTableMaster() {
        return (TableMaster) mealCount -> getLocalTable().getTableMaster().isAllowedToTakeSeat(mealCount) &&
                getTables().skip(1).map(Table::getTableMaster)
                        .allMatch(tableMaster -> tableMaster.isAllowedToTakeSeat(mealCount));
    }

    @Override
    public void setTableMaster(TableMaster tableMaster) {
        getLocalTable().setTableMaster(tableMaster);
    }

    @Override
    public BackupService getBackupService() {
        throw new UnsupportedOperationException();
    }

    public void addTable(final String host) throws RemoteException {
        connectToTable(host);
    }

    public void removeTable(final String host) throws RemoteException {
        disconnectFromTable(host);
    }

    public void addPhilosopher(final String host, final String name, final boolean hungry) throws RemoteException {
        getTables().parallel()
                .skip(1)
                .filter(table -> table.getName().equals(host))
                .findAny()
                .ifPresent(table -> table.getBackupService().addPhilosopher(name, hungry));
    }

    public void removePhilosopher(final String host, final String name) throws RemoteException {
        getTables().parallel()
                .skip(1)
                .filter(table -> table.getName().equals(host))
                .findAny()
                .ifPresent(table -> table.getBackupService().removePhilosopher(name));
    }

    public void addChair(final String host, final String name) throws RemoteException {
        getTables().parallel()
                .skip(1)
                .filter(table -> table.getName().equals(host))
                .findAny()
                .ifPresent(table -> table.getBackupService().addChair(name));
    }

    public void removeChair(final String host, final String name) throws RemoteException {
        getTables().parallel()
                .skip(1)
                .filter(table -> table.getName().equals(host))
                .findAny()
                .ifPresent(table -> table.getBackupService().removeChair(name));
    }

    public Chair getChair(final int index) throws RemoteException {
        return getChairs().collect(Collectors.toList()).get(index);
    }

    public int getChairCount() throws RemoteException {
        return (int) getChairs().count();
    }

    public TableMaster getMaster() throws RemoteException {
        return getTableMaster();
    }

    @Override
    public void update(Observable observable, Object object) {
        final Table table = (Table) object; // This table as been disconnected!
        logger.log("Unreachable table " + table.getName() + " detected...");

        System.out.println("Unreachable table '" + table.getName() + "'");
        System.out.println("Chair(s):");
        table.getBackupService().getChairs().forEach(tmp -> System.out.println("\t- " + tmp.toString()));
        System.out.println("Philosopher(s):");
        table.getBackupService().getPhilosophers().map(Philosopher::getName).map(name -> "\t- " + name).forEach(System.out::println);

        /*
        tables.remove(table); // Remove the disconnected table

        logger.log("Try to restore unreachable table " + table.getName() + "...");
        if (tables.size() == 1) {
            // This table is the last one -> backup the lost one
            getBackupService().restoreBackup(table.getName());
            getTables().parallel()
                    .map(Table::getBackupService)
                    .forEach(backupService -> backupService.deleteBackup(table.getName()));
            logger.log("Restored unreachable table " + table.getName() + "!");
        } else {
            // Searching for a table which is not occupied enough -> hopefully it's not me!
            final int occupationRemote = (int) (getTables().map(Table::getBackupService)
                    .map(BackupService::createBackup)
                    .parallel()
                    .mapToDouble(remoteBackup -> remoteBackup.getCores() / remoteBackup.getPhilosopherCount())
                    .max()
                    .getAsDouble() * 100);
            final Backup backup = getBackupService().createBackup();
            final int occupationLocal = (int) (backup.getCores() / (double) backup.getPhilosopherCount() * 100);

            // Check whether this table has less occupation then others
            if (occupationRemote == occupationLocal) {
                // This table is the right -> start restoring the backup
                getBackupService().restoreBackup(table.getName());
                getTables().parallel()
                        .map(Table::getBackupService)
                        .forEach(backupService -> backupService.deleteBackup(table.getName()));
                logger.log("Restored unreachable table " + table.getName() + "!");
            } else {
                logger.log("Unreachable table " + table.getName() + " will be restored by another one!");
            }
        }
        */
    }

    private Table getLocalTable() {
        return localTable;
    }
}
