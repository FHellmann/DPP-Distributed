package edu.hm.cs.vss.rmi;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.stream.Stream;

/**
 * Created by Fabio Hellmann on 13.04.2016.
 */
public interface RmiDistributedController extends Remote {
    default String getHostName() throws RemoteException {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RemoteException("", e);
        }
    }

    void register(final String host) throws RemoteException;

    void unregister(final String host) throws RemoteException;

    Stream<RmiChair> getFreeChairs(final RmiPhilosopherStatus philosopherStatus) throws RemoteException;

    RmiChair getNeighbourChair(final RmiChair chair) throws RemoteException;

    RmiPhilosopherStatus getStatus() throws RemoteException;
}
