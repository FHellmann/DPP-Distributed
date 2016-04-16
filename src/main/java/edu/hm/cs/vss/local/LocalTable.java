package edu.hm.cs.vss.local;

import edu.hm.cs.vss.Chair;
import edu.hm.cs.vss.Table;
import edu.hm.cs.vss.TableMaster;
import edu.hm.cs.vss.log.EmptyLogger;
import edu.hm.cs.vss.log.Logger;
import edu.hm.cs.vss.remote.RemoteTable;
import edu.hm.cs.vss.remote.RmiTable;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by Fabio Hellmann on 14.04.2016.
 */
public class LocalTable extends UnicastRemoteObject implements RmiTable, Table {
    private final List<Table> tables = Collections.synchronizedList(new LinkedList<>());
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
                final Table table = new RemoteTable(tableHost, logger);
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
    public TableMaster getMaster() throws RemoteException {
        return getTableMaster();
    }
}
