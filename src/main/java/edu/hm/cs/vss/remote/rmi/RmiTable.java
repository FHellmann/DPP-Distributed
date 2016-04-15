package edu.hm.cs.vss.remote.rmi;

import edu.hm.cs.vss.Chair;
import edu.hm.cs.vss.TableMaster;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Created by Fabio Hellmann on 14.04.2016.
 */
public interface RmiTable extends Remote {

    void addTable(final String host) throws RemoteException;

    void removeTable(final String host) throws RemoteException;

    List<Chair> getChairs() throws RemoteException;

    TableMaster getTableMaster() throws RemoteException;

    static RmiTable create() {
        return new RmiTable() {
            @Override
            public void addTable(String host) throws RemoteException {

            }

            @Override
            public void removeTable(String host) throws RemoteException {

            }

            @Override
            public List<Chair> getChairs() throws RemoteException {
                return null;
            }

            @Override
            public TableMaster getTableMaster() throws RemoteException {
                return null;
            }
        };
    }
}
