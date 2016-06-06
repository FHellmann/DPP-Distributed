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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by fhellman on 18.04.2016.
 */
public class LocalTablePool implements RmiTable, Table, Observer {
    private final List<Table> tables = Collections.synchronizedList(new LinkedList<>());
    private final List<Philosopher> localPhilosophers = Collections.synchronizedList(new ArrayList<>());
    private final Table localTable;
    private final TableMaster tableMaster;
    private final Logger logger;
    private final AtomicBoolean backupLock = new AtomicBoolean();

    public LocalTablePool() throws IOException {
        this(new DummyLogger());
    }

    public LocalTablePool(final Logger logger) throws IOException {
        this.localTable = new LocalTable(logger);
        this.tableMaster = new DistributedTableMaster();
        this.logger = logger;

        final Registry registry = LocateRegistry.createRegistry(NETWORK_PORT);
        registry.rebind(Table.class.getSimpleName(), UnicastRemoteObject.exportObject(this, NETWORK_PORT));

        tables.add(localTable);
    }

    @Override
    public void connectToTable(final String tableHost) {
        logger.log("Try to connect to remote table " + tableHost + "...");
        if (tables.parallelStream().noneMatch(table -> table.getName().equals(tableHost))) {
            try {
                final RemoteTable table = new RemoteTable(tableHost, logger);
                table.addObserver(this); // Observe table for disconnection
                tables.add(table);

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
                .ifPresent(table -> table.getBackupService().addPhilosopher(this, name, hungry, takenMeals));
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
        return false;
    }

    @Override
    public void update(Observable observable, Object object) {

        // Only allow one thread to work here
        if (!backupLock.compareAndSet(false, true)) {
            logger.log("Backup is already locked, bye");
            return;
        }

        logger.log("suspending all local philosophers");
        localTable.getPhilosophers().forEach(Philosopher::putToSleep);

        final Table table = (Table) object; // This table as been disconnected!

        // There are enough tables to even consider that we are not responsible
        if (tables.size() >= 2) {
            // We are NOT on the left side of the dead table
            if (!tables.get(1).getName().equals(table.getName())) {
                logger.log(table.getName() + " is not on the right of our table, yeah");

                RemoteTable restoringTableTemp = null;
                for (int i = 1; i < tables.size(); i++) {
                    if (tables.get(i).getName().equals(table.getName())) {
                        restoringTableTemp = (RemoteTable) tables.get(i + 1);
                        break;
                    }
                }

                assert (restoringTableTemp != null);

                final RemoteTable restoringTable = restoringTableTemp;

                (new Thread() {
                    public void run() {
                        while (true) {
                            try {
                                if(restoringTable.getRmi().backupFinished()){
                                    localTable.getPhilosophers().forEach(Philosopher::wakeUp);
                                    break;
                                }
                                Thread.sleep(1000);
                            } catch (RemoteException e) {
                                restoringTable.handleRemoteTableDisconnected(e);
                            } catch (InterruptedException e) {
                                // ignore exception
                            }
                        }
                    }
                }).start();

                return;
            }
        }
        // "else" - either we are the only table left, or we are responsible for backing up

        final BackupService tableBackupService = table.getBackupService();
        logger.log("Unreachable table " + table.getName() + " detected...");

        logger.log("Chair(s):");
        tableBackupService.getChairs().forEach(tmp -> logger.log("\t- " + tmp.toString()));
        logger.log("Philosopher(s):");
        tableBackupService.getPhilosophers().map(Philosopher::getName).map(name -> "\t- " + name).forEach(logger::log);

        tables.remove(table); // Remove the disconnected table

        logger.log("Try to restore unreachable table " + table.getName() + "...");
        tableBackupService.getChairs().forEach(this::addChair);
        tableBackupService.getPhilosophers().forEach(this::addPhilosopher);
        logger.log("Restored unreachable table " + table.getName() + "!");
    }

    private Table getLocalTable() {
        return localTable;
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
}
