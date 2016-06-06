package edu.hm.cs.vss.remote;

import edu.hm.cs.vss.Chair;
import edu.hm.cs.vss.Fork;
import edu.hm.cs.vss.TableMaster;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Optional;

/**
 * Created by Fabio on 16.04.2016.
 */
public interface RmiTable extends Remote {
    void addTable(final String host) throws RemoteException;

    void removeTable(final String host) throws RemoteException;

    void addPhilosopher(final String host, final String name, final boolean hungry, final int takenMeals) throws RemoteException;

    void removePhilosopher(final String host, final String name) throws RemoteException;

    void onStandUp(final String host, final String philosopherName) throws RemoteException;

    void addChair(final String host, final String name) throws RemoteException;

    void removeChair(final String host, final String name) throws RemoteException;

    Chair getChair(final int index) throws RemoteException;

    int getChairCount() throws RemoteException;

    boolean blockChairIfAvailable(final String name) throws RemoteException;

    void unblockChair(final String name) throws RemoteException;

    boolean blockForkIfAvailable(final String name) throws RemoteException;

    void unblockFork(final String name) throws RemoteException;

    int getChairWaitingPhilosophers(final String name) throws RemoteException;

    boolean backupFinished() throws RemoteException;
}
