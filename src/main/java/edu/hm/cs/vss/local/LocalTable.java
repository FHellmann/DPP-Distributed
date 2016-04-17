package edu.hm.cs.vss.local;

import edu.hm.cs.vss.*;
import edu.hm.cs.vss.log.EmptyLogger;
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
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by Fabio Hellmann on 14.04.2016.
 */
public class LocalTable extends UnicastRemoteObject implements RmiTable, Table, Observer {
    private final List<Table> tables = Collections.synchronizedList(new LinkedList<>());
    private final List<Philosopher> philosophers = Collections.synchronizedList(new ArrayList<>());
    private final List<Chair> chairs = Collections.synchronizedList(new ArrayList<>());
    private final Logger logger;
    private TableMaster tableMaster;

    public LocalTable() throws IOException {
        this(new EmptyLogger());
    }

    public LocalTable(final Logger logger) throws IOException {
        super(STATIC_PORT);
        this.logger = logger;
        final Registry registry = LocateRegistry.createRegistry(STATIC_PORT);
        registry.rebind(Table.class.getSimpleName(), this);
        tables.add(this);
    }

    @Override
    public void addTable(final String tableHost) {
        logger.log("Try to connect to remote table " + tableHost + "...");
        if (tables.parallelStream().noneMatch(table -> table.getName().equals(tableHost))) {
            try {
                final RemoteTable table = new RemoteTable(tableHost, logger);
                table.addObserver(this);
                tables.add(table);
                tables.stream()
                        .map(Table::getName)
                        .filter(name -> !name.equals(tableHost)) // Skip out the previous added table
                        .forEach(table::addTable);
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
    public void removeTable(final String tableHost) {
        tables.parallelStream()
                .skip(1) // Skip the local table
                .peek(table -> table.removeTable(tableHost))
                .filter(table -> table.getName().equals(tableHost))
                .findAny()
                .ifPresent(host -> {
                    logger.log("Try to disconnect from remote table " + tableHost + "...");
                    tables.remove(host);
                    logger.log("Disconnected from remote table " + tableHost + "!");
                });
    }

    @Override
    public Chair getChair(int index) throws RemoteException {
        return chairs.get(index);
    }

    @Override
    public int getChairCount() throws RemoteException {
        return chairs.size();
    }

    @Override
    public Stream<Table> getTables() {
        return tables.stream();
    }

    @Override
    public Philosopher addPhilosopher(final Philosopher philosopher) {
        philosophers.add(philosopher);
        philosopher.start();
        return philosopher;
    }

    @Override
    public void removePhilosopher(Philosopher philosopher) {
        philosopher.interrupt();
        philosophers.remove(philosopher);
    }

    @Override
    public Stream<Philosopher> getPhilosophers() {
        return philosophers.stream();
    }

    @Override
    public void addChair(Chair chair) {
        chairs.add(chair);
    }

    @Override
    public void removeChair(Chair chair) {
        chairs.remove(chair);
    }

    @Override
    public Stream<Chair> getChairs() {
        return chairs.stream();
    }

    @Override
    public void setTableMaster(TableMaster tableMaster) {
        if (tableMaster != null) {
            this.tableMaster = tableMaster;
        }
    }

    @Override
    public TableMaster getTableMaster() {
        return tableMaster;
    }

    @Override
    public Backup getBackup() {
        final int cpuCores = Runtime.getRuntime().availableProcessors();
        final int chairCount = chairs.size();
        final List<String> philosopherNames = philosophers.stream().map(Philosopher::getName).collect(Collectors.toList());
        final List<Integer> philosopherMealCount = philosophers.stream().map(Philosopher::getMealCount).collect(Collectors.toList());
        final List<Boolean> philosopherHungry = philosophers.stream().map(Philosopher::isHungry).collect(Collectors.toList());
        return new LocalBackup(cpuCores, chairCount, philosopherNames, philosopherMealCount, philosopherHungry);
    }

    public Backup getBackupDetails() throws RemoteException {
        return getBackup();
    }

    private void restoreTable(final Table table) {
        logger.log("Restoring table " + table.getName() + "...");
        final Backup backup = table.getBackup();
        IntStream.rangeClosed(1, backup.getChairCount())
                .mapToObj(index -> new Chair.Builder().setNameUniqueId().create())
                .forEach(this::addChair);

        IntStream.rangeClosed(0, backup.getPhilosopherCount() - 1)
                .mapToObj(index -> new Philosopher.Builder()
                        .setTable(this)
                        .name(backup.getPhilosopherName(index))
                        .setTakenMeals(backup.getPhilosopherMealCount(index))
                        .setVeryHungry(backup.isPhilosopherHungry(index))
                        .setFileLogger()
                        .create())
                .forEach(this::addPhilosopher);
        logger.log("Restored table " + table.getName() + " at this " + getName() + " table!");
    }

    @Override
    public TableMaster getMaster() throws RemoteException {
        return getTableMaster();
    }

    @Override
    public void update(Observable observable, Object object) {
        final Table table = (Table) object; // This table as been disconnected!
        tables.remove(table); // Remove the disconnected table

        if (tables.size() == 1) {
            // This table is the last one -> backup the lost one
            restoreTable(table);
        } else {
            // Searching for a table which is not occupied enough -> hopefully it's not me!
            final int occupationRemote = (int) (getTables().map(Table::getBackup).parallel()
                    .mapToDouble(remoteBackup -> remoteBackup.getCores() / remoteBackup.getPhilosopherCount()).max().getAsDouble() * 100);
            final int occupationLocal = (int) (getBackup().getCores() / (double) getBackup().getPhilosopherCount() * 100);

            // Check whether this table has less occupation then others
            if (occupationRemote == occupationLocal) {
                // This table is the right -> start restoring the backup
                restoreTable(table);
            }
        }
    }

    private static class LocalBackup implements Backup {
        private final int cpuCores;
        private final int chairCount;
        private final List<String> philosopherNames;
        private final List<Integer> philosopherMealCount;
        private final List<Boolean> philosopherHungry;

        public LocalBackup(int core, int chairs, List<String> names, List<Integer> meals, List<Boolean> hungry) {
            cpuCores = core;
            chairCount = chairs;
            philosopherNames = names;
            philosopherMealCount = meals;
            philosopherHungry = hungry;
        }

        @Override
        public int getCores() {
            return cpuCores;
        }

        @Override
        public int getChairCount() {
            return chairCount;
        }

        @Override
        public int getPhilosopherCount() {
            return philosopherNames.size();
        }

        @Override
        public String getPhilosopherName(int index) {
            return philosopherNames.get(index);
        }

        @Override
        public int getPhilosopherMealCount(int index) {
            return philosopherMealCount.get(index);
        }

        @Override
        public boolean isPhilosopherHungry(int index) {
            return philosopherHungry.get(index);
        }
    }
}
