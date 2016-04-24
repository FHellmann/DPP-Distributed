package edu.hm.cs.vss.remote;

import edu.hm.cs.vss.*;
import edu.hm.cs.vss.local.LocalTablePool;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Fabio on 16.04.2016.
 */
public interface RmiTable extends Remote {
    void addTable(final String host) throws RemoteException;

    void removeTable(final String host) throws RemoteException;

    void addPhilosopher(final String host, final String name, final boolean hungry) throws RemoteException;

    void removePhilosopher(final String host, final String name) throws RemoteException;

    void addChair(final String host, final String name) throws RemoteException;

    void removeChair(final String host, final String name) throws RemoteException;

    Chair getChair(final int index) throws RemoteException;

    int getChairCount() throws RemoteException;

    TableMaster getMaster() throws RemoteException;
}
