package edu.hm.cs.vss.remote;

import edu.hm.cs.vss.Backup;
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

    default Chair getChair(final int index) throws RemoteException {
        return getChairs().collect(Collectors.toList()).get(index);
    }

    default int getChairCount() throws RemoteException {
        return (int) getChairs().count();
    }

    default TableMaster getMaster() throws RemoteException {
        return getTableMaster();
    }

    default Backup getBackupDetails() throws RemoteException {
        return getBackupService().createBackup();
    }

    default void safeBackup(final Backup backup) throws RemoteException {
        getBackupService().storeBackup(backup);
    }

    default void deleteBackup(final String host) throws RemoteException {
        getBackupService().deleteBackup(host);
    }
}
