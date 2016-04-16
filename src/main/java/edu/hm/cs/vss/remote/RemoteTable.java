package edu.hm.cs.vss.remote;

import edu.hm.cs.vss.Chair;
import edu.hm.cs.vss.Table;
import edu.hm.cs.vss.TableMaster;
import edu.hm.cs.vss.log.Logger;

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
    private final Logger logger;

    public RemoteTable(final String host, Logger logger) throws Exception {
        this.host = host;
        this.logger = logger;
        table = new RemoteRmiTable(host);
    }

    @Override
    public String getName() {
        return host;
    }

    @Override
    public void addTable(final String tableHost) {
        try {
            logger.log("Requesting the remote table " + table.getRef().remoteToString() + " to add the table " + tableHost);
            table.addTable(tableHost);
        } catch (RemoteException e) {
            logger.log(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void removeTable(final String tableHost) {
        try {
            logger.log("Requesting the remote table " + table.getRef().remoteToString() + " to delete the table " + tableHost);
            table.removeTable(tableHost);
        } catch (RemoteException e) {
            logger.log(e.getMessage());
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
            logger.log("Requesting chairs from remote table " + table.getRef().remoteToString());
            return IntStream.rangeClosed(0, table.getChairCount() - 1)
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
            logger.log(e.getMessage());
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
        logger.log("Requesting the remote table " + table.getRef().remoteToString() + " to check if the philosopher is allowed to take a seat");
        try {
            return table.getMaster();
        } catch (RemoteException e) {
            logger.log(e.getMessage());
            e.printStackTrace();
        }
        return (TableMaster) philosopher -> true;
    }

    private static class RemoteRmiTable extends UnicastRemoteObject implements RmiTable {
        private final RmiTable table;

        RemoteRmiTable(final String host) throws RemoteException, NotBoundException {
            super(STATIC_PORT);
            final Registry registry = LocateRegistry.getRegistry(host, STATIC_PORT);
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
        public TableMaster getMaster() throws RemoteException {
            return table.getMaster();
        }
    }
}
