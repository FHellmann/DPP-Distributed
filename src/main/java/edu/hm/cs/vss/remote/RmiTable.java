package edu.hm.cs.vss.remote;

import edu.hm.cs.vss.Chair;
import edu.hm.cs.vss.Table;
import edu.hm.cs.vss.TableMaster;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.stream.Collectors;

/**
 * Created by Fabio on 16.04.2016.
 */
public interface RmiTable extends Remote, Table {
    default void addTable(final String host) throws RemoteException {
        connectToTable(host);
    }

    default void removeTable(final String host) throws RemoteException {
        disconnectFromTable(host);
    }

    default void addPhilosopher(final String host, final String name, final boolean hungry) throws RemoteException {
        getTables().parallel()
                .skip(1)
                .filter(table -> table.getName().equals(host))
                .findAny()
                .ifPresent(table -> table.getBackupService().addPhilosopher(name, hungry));
    }

    default void removePhilosopher(final String host, final String name) throws RemoteException {
        getTables().parallel()
                .skip(1)
                .filter(table -> table.getName().equals(host))
                .findAny()
                .ifPresent(table -> table.getBackupService().removePhilosopher(name));
    }

    default void addChair(final String host, final String name) throws RemoteException {
        getTables().parallel()
                .skip(1)
                .filter(table -> table.getName().equals(host))
                .findAny()
                .ifPresent(table -> table.getBackupService().addChair(name));
    }

    default void removeChair(final String host, final String name) throws RemoteException {
        getTables().parallel()
                .skip(1)
                .filter(table -> table.getName().equals(host))
                .findAny()
                .ifPresent(table -> table.getBackupService().removeChair(name));
    }

    default Chair getChair(final int index) throws RemoteException {
        return getChairs().collect(Collectors.toList()).get(index);
    }

    default int getChairCount() throws RemoteException {
        return (int) getChairs().count();
    }

    default TableMaster getMaster() throws RemoteException {
        return getTableMaster();
    }
}
