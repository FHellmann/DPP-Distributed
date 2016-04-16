package edu.hm.cs.vss.local;

import edu.hm.cs.vss.Chair;
import edu.hm.cs.vss.Table;
import edu.hm.cs.vss.TableMaster;
import edu.hm.cs.vss.remote.RmiTable;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by Fabio Hellmann on 14.04.2016.
 */
public class LocalTable extends UnicastRemoteObject implements RmiTable, Table {
    private static final TableMaster DEFAULT_TABLE_MASTER = philosopher -> true;
    private final List<Chair> chairs = Collections.synchronizedList(new ArrayList<>());
    private TableMaster tableMaster = DEFAULT_TABLE_MASTER;

    LocalTable() throws RemoteException {
        super(STATIC_PORT);
        final Registry registry = LocateRegistry.createRegistry(STATIC_PORT);
        registry.rebind(Table.class.getSimpleName(), this);
    }

    @Override
    public void addTable(final String tableHost) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeTable(final String tableHost) {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    @Override
    public void addChair(Chair chair) {
        chairs.add(chair);
    }

    @Override
    public void removeChair(Chair chair) {
        if (chair.isAvailable()) {
            chairs.remove(chair);
        }
    }

    @Override
    public Stream<Chair> getChairs() {
        return chairs.stream();
    }

    @Override
    public void setTableMaster(TableMaster tableMaster) {
        if (tableMaster == null) {
            this.tableMaster = DEFAULT_TABLE_MASTER;
        } else {
            this.tableMaster = tableMaster;
        }
    }

    @Override
    public TableMaster getTableMaster() {
        return tableMaster;
    }
}
