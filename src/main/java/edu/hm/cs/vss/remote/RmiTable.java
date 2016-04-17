package edu.hm.cs.vss.remote;

import edu.hm.cs.vss.Backup;
import edu.hm.cs.vss.Chair;
import edu.hm.cs.vss.TableMaster;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by Fabio on 16.04.2016.
 */
public interface RmiTable extends Remote {
    void addTable(final String host) throws RemoteException;

    void removeTable(final String host) throws RemoteException;

    Chair getChair(final int index) throws RemoteException;

    int getChairCount() throws RemoteException;

    TableMaster getMaster() throws RemoteException;

    Backup getBackupDetails() throws RemoteException;
}
