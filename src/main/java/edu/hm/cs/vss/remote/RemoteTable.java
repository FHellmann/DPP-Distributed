package edu.hm.cs.vss.remote;

import edu.hm.cs.vss.Chair;
import edu.hm.cs.vss.Table;
import edu.hm.cs.vss.TableMaster;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by Fabio Hellmann on 14.04.2016.
 */
public class RemoteTable implements Table {

    private final RemoteRmiTable table;
    private final String host;

    public RemoteTable(final String host) throws Exception {
        this.host = host;
        table = new RemoteRmiTable(host);
    }

    @Override
    public String getName() {
        return host;
    }

    @Override
    public void addTable(final String tableHost) {
        try {
            table.addTable(tableHost);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeTable(final String tableHost) {
        try {
            table.removeTable(tableHost);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Stream<Table> getTables() {
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
            return IntStream.rangeClosed(0, table.getChairCount())
                    .mapToObj(index -> {
                        try {
                            return table.getChair(index);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        return null;
                    })
                    .filter(chair -> chair != null);
        } catch (RemoteException e) {
            e.printStackTrace();
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
        return (TableMaster) philosopher -> {
            try {
                return table.getTableMaster().isAllowedToTakeSeat(philosopher);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return true;
        };
    }

    private static class RemoteRmiTable extends UnicastRemoteObject implements RmiTable {

        private final Registry registry;
        private final RmiTable table;

        RemoteRmiTable(final String host) throws RemoteException, NotBoundException {
            super(STATIC_PORT);
            registry = LocateRegistry.getRegistry(host, STATIC_PORT);
            table = (RmiTable) registry.lookup(Table.class.getSimpleName());
        }

        @Override
        public void addTable(String host) throws RemoteException {
            table.addTable(host);
        }

        @Override
        public void removeTable(String host) throws RemoteException {
            table.removeTable(host);
        }

        @Override
        public Chair getChair(int index) throws RemoteException {
            return table.getChair(index);
        }

        @Override
        public int getChairCount() throws RemoteException {
            return table.getChairCount();
        }

        @Override
        public TableMaster getTableMaster() throws RemoteException {
            return table.getTableMaster();
        }
    }
}
