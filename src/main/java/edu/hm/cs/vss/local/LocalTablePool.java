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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Created by fhellman on 18.04.2016.
 */
public class LocalTablePool implements Table {
    private final List<Table> tables = Collections.synchronizedList(new LinkedList<>());
    private final List<Table> backedUpTables = Collections.synchronizedList(new LinkedList<>());
    private final List<Philosopher> localPhilosophers = Collections.synchronizedList(new ArrayList<>());
    private final Table localTable;
    private final TableMaster tableMaster;
    private final BackupRestorer tableBrokeUpObserver;
    private final Logger logger;
    private final AtomicBoolean backupLock = new AtomicBoolean();

    public LocalTablePool() throws IOException {
        this(new DummyLogger());
    }

    public LocalTablePool(final Logger logger) throws IOException {
        this.localTable = new LocalTable(logger);
        this.tableMaster = new DistributedTableMaster();
        this.tableBrokeUpObserver = new BackupRestorer();
        this.logger = logger;

        final Registry registry = LocateRegistry.createRegistry(NETWORK_PORT);
        registry.rebind(Table.class.getSimpleName(), UnicastRemoteObject.exportObject(new DistributedTableRmi(), NETWORK_PORT));

        tables.add(localTable);

        (new Thread() {
            public void run() {
                while (true) {
                    try {
                        if (!backupLock.get()) {
                            tables.stream().skip(1).map(table -> (RemoteTable) table).forEach(table -> {
                                try {
                                    table.backupFinished();
                                } catch (RemoteException e) {
                                    table.handleRemoteTableDisconnected(e);
                                    throw new RuntimeException(e);
                                }
                            });
                        }
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        // ignore exception
                    }
                }
            }
        }).start();
    }

    @Override
    public void connectToTable(final String tableHost) {
        logger.log("Try to connect to remote table " + tableHost + "...");
        if (tables.parallelStream().noneMatch(table -> table.getName().equals(tableHost))) {
            try {
                final RemoteTable table = new RemoteTable(tableHost, logger);
                table.addObserver(tableBrokeUpObserver); // Observe table for disconnection
                tables.add(table);

                // Removes backed up table on reconnection
                for (Iterator<Table> iter = backedUpTables.listIterator(); iter.hasNext(); ) {
                    RemoteTable t = (RemoteTable) iter.next();
                    if (t.getHost().equals(tableHost)) {
                        iter.remove();
                    }
                }

                // Send all available tables to the new table
                tables.stream()
                        .map(Table::getName)
                        .filter(name -> !name.equals(tableHost)) // Skip out the previous added table
                        .forEach(table::connectToTable);

                // Backup the current status of this table and send it to the new table
                getLocalTable().getChairs().forEach(table::addChair);
                getPhilosophers().forEach(table::addPhilosopher);

                // Table is ready to use
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
        if (tables.parallelStream().noneMatch(table -> table.getName().equals(tableHost))) {
            tables.parallelStream()
                    .skip(1) // Skip the current table -> it's already execution this statement
                    .peek(table -> table.disconnectFromTable(tableHost))
                    .filter(table -> table.getName().equals(tableHost))
                    .findAny()
                    .ifPresent(host -> {
                        tables.remove(host);
                        logger.log("Disconnected from remote table " + tableHost + "!");
                    });
        } else {
            logger.log("Already disconnected from " + tableHost + "!");
        }
    }

    @Override
    public Stream<Table> getTables() {
        return tables.stream();
    }

    @Override
    public void addPhilosopher(final Philosopher philosopher) {
        localPhilosophers.add(philosopher);
        philosopher.start();
        philosopher.addOnStandUpListener(tmp -> getTables().parallel()
                .skip(1)
                .map(table -> (Philosopher.OnStandUpListener) table)
                .forEach(listener -> listener.onStandUp(philosopher)));

        // Inform other tables
        getTables().parallel()
                .skip(1)
                .forEach(table -> table.addPhilosopher(philosopher));
    }

    @Override
    public void removePhilosopher(Philosopher philosopher) {
        philosopher.interrupt();
        localPhilosophers.remove(philosopher);

        // Inform other tables
        getTables().parallel()
                .skip(1)
                .forEach(table -> table.removePhilosopher(philosopher));
    }

    @Override
    public Stream<Philosopher> getPhilosophers() {
        return localPhilosophers.stream();
    }

    @Override
    public void addChair(Chair chair) {
        getLocalTable().addChair(chair);

        // Inform other tables
        getTables().parallel()
                .skip(1)
                .forEach(table -> table.addChair(chair));
    }

    @Override
    public void removeChair(Chair chair) {
        getLocalTable().removeChair(chair);

        // Inform other tables
        getTables().parallel()
                .skip(1)
                .forEach(table -> table.removeChair(chair));
    }

    @Override
    public Stream<Chair> getChairs() {
        return tables.stream().flatMap(Table::getChairs);
    }

    @Override
    public TableMaster getTableMaster() {
        return tableMaster;
    }

    @Override
    public void setTableMaster(TableMaster tableMaster) {
        getLocalTable().setTableMaster(tableMaster);
    }

    @Override
    public BackupService getBackupService() {
        throw new UnsupportedOperationException();
    }

    private Table getLocalTable() {
        return localTable;
    }

    private final class DistributedTableRmi implements RmiTable {
        public void addTable(final String host) throws RemoteException {
            connectToTable(host);
        }

        public void removeTable(final String host) throws RemoteException {
            disconnectFromTable(host);
        }

        public void addPhilosopher(final String host, final String name, final boolean hungry, final int takenMeals) throws RemoteException {
            getTables().parallel()
                    .skip(1)
                    .filter(table -> table.getName().equals(host))
                    .findAny()
                    .ifPresent(table -> table.getBackupService().addPhilosopher(LocalTablePool.this, name, hungry, takenMeals));
        }

        public void removePhilosopher(final String host, final String name) throws RemoteException {
            getTables().parallel()
                    .skip(1)
                    .filter(table -> table.getName().equals(host))
                    .findAny()
                    .ifPresent(table -> table.getBackupService().removePhilosopher(name));
        }

        @Override
        public void onStandUp(String host, String philosopherName) throws RemoteException {
            getTables().parallel()
                    .skip(1)
                    .filter(table -> table.getName().equals(host))
                    .findAny()
                    .ifPresent(table -> table.getBackupService().onPhilosopherStandUp(philosopherName));
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

        @Override
        public boolean blockChairIfAvailable(String name) throws RemoteException {
            return getLocalTable().getChairs().parallel()
                    .filter(chair -> chair.toString().equals(name))
                    .findAny()
                    .flatMap(chair -> {
                        try {
                            return chair.blockIfAvailable();
                        } catch (InterruptedException e) {
                            return Optional.empty();
                        }
                    })
                    .isPresent();
        }

        @Override
        public void unblockChair(String name) throws RemoteException {
            getLocalTable().getChairs().parallel()
                    .filter(chair -> chair.toString().equals(name))
                    .findAny()
                    .ifPresent(Chair::unblock);
        }

        @Override
        public boolean blockForkIfAvailable(String name) throws RemoteException {
            return getLocalTable().getChairs().parallel()
                    .map(Chair::getFork)
                    .filter(fork -> fork.toString().equals(name))
                    .findAny()
                    .flatMap(Fork::blockIfAvailable)
                    .isPresent();
        }

        @Override
        public void unblockFork(String name) throws RemoteException {
            getLocalTable().getChairs().parallel()
                    .map(Chair::getFork)
                    .filter(fork -> fork.toString().equals(name))
                    .findAny()
                    .ifPresent(Fork::unblock);
        }

        @Override
        public int getChairWaitingPhilosophers(String name) throws RemoteException {
            return getLocalTable().getChairs().parallel()
                    .filter(chair -> chair.toString().equals(name))
                    .findAny()
                    .map(Chair::getWaitingPhilosopherCount)
                    .orElse(0);
        }

        @Override
        public boolean backupFinished() throws RemoteException {
            return !backupLock.get();
        }
    }

    private final class DistributedTableMaster extends LocalTableMaster {
        @Override
        public void register(Philosopher philosopher) {
            getLocalTable().getTableMaster().register(philosopher);
        }

        @Override
        public void unregister(Philosopher philosopher) {
            getLocalTable().getTableMaster().unregister(philosopher);
        }

        @Override
        public void onStandUp(Philosopher philosopher) {
            super.onStandUp(philosopher);
            // Inform all other tables that this philosopher stands up
            getTables().skip(1)
                    .map(table -> (Philosopher.OnStandUpListener) table)
                    .forEach(listener -> listener.onStandUp(philosopher));
        }

        @Override
        public boolean isAllowedToTakeSeat(Integer mealCount) {
            return getTables()
                    .filter(table -> table.getPhilosophers().count() > 0)
                    .map(Table::getTableMaster)
                    .allMatch(master -> master.isAllowedToTakeSeat(mealCount));
        }
    }

    private final class BackupRestorer extends Thread implements Observer {
        @Override
        public void run() {
            super.run();
        }

        @Override
        public void update(Observable observable, Object object) {

            // Only allow one thread to work here
            if (!backupLock.compareAndSet(false, true)) {
                return;
            }

            synchronized (tables) {
                final Table table = (Table) object; // This table as been disconnected!
                logger.log("Unreachable table " + table.getName() + " detected...");

                if (backedUpTables.stream().anyMatch(t -> table.getName().equals(t.getName()))) {
                    logger.log("table " + table.getName() + " already backed up, exiting");
                    return;
                }

                logger.log("suspending all local philosophers");
                getPhilosophers().forEach(Philosopher::putToSleep);

                // There are enough tables to even consider that we are not responsible
                if (tables.size() > 2 && !tables.get(1).getName().equals(table.getName())) {
                    // We are NOT on the left side of the dead table
                    logger.log(table.getName() + " is not on the right of our table, yeah");

                    RemoteTable restoringTableTemp = (RemoteTable) tables.get(tables.indexOf(table) - 1);

                    (new Thread() {
                        public void run() {
                            // TODO: Set lock in restoringTable
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            try {
                                while (!restoringTableTemp.backupFinished()) {
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        // ignore exception
                                    }
                                }
                            } catch (RemoteException e) {
                                restoringTableTemp.handleRemoteTableDisconnected(e);
                            }
                            backupLock.set(false);
                            getPhilosophers().forEach(Philosopher::wakeUp);
                        }
                    }).start();
                    return;
                }
                // "else" - either we are the only table left, or we are responsible for backing up

                final BackupService tableBackupService = table.getBackupService();

                logger.log("Chair(s):");
                tableBackupService.getChairs().forEach(tmp -> logger.log("\t- " + tmp.toString()));
                logger.log("Philosopher(s):");
                tableBackupService.getPhilosophers().map(Philosopher::getName).map(name -> "\t- " + name).forEach(logger::log);

                tables.remove(table); // Remove the disconnected table

                logger.log("Try to restore unreachable table " + table.getName() + "...");
                tableBackupService.restoreTo(LocalTablePool.this);
                logger.log("Restored unreachable table " + table.getName() + "!");

                backedUpTables.add(table);

                getPhilosophers().forEach(Philosopher::wakeUp);
                backupLock.set(false);
            }
        }
    }
}
