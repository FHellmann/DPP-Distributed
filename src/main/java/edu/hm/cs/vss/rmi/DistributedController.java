package edu.hm.cs.vss.rmi;

import edu.hm.cs.vss.Table;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created by Fabio Hellmann on 13.04.2016.
 */
public class DistributedController extends UnicastRemoteObject implements RmiDistributedController {
    private final List<RmiDistributedController> networkControllers = Collections.synchronizedList(new ArrayList<>());
    private final Table tableSegment;

    private DistributedController(final Table table) throws RemoteException {
        tableSegment = table;
    }

    public void connect() throws Exception {
        try {
            final Optional<InetAddress> address = Collections.list(NetworkInterface.getNetworkInterfaces()).parallelStream()
                    .flatMap(networkInterface -> Collections.list(networkInterface.getInetAddresses()).parallelStream())
                    .filter(inetAddress -> !inetAddress.isLoopbackAddress())
                    .filter(inetAddress -> !inetAddress.isAnyLocalAddress())
                    .filter(inetAddress -> !inetAddress.isMulticastAddress())
                    .filter(inetAddress -> {
                        try {
                            Registry registry = LocateRegistry.getRegistry(inetAddress.getHostAddress());
                            registry.lookup(Table.class.getSimpleName());
                            return true;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .findAny();

            if (address.isPresent()) {
                // Server found...
                final Registry registry = LocateRegistry.getRegistry(address.get().getHostAddress());
                networkControllers.add((RmiDistributedController) registry.lookup(RmiDistributedController.class.getSimpleName()));
            } else {
                // Open Server...
                final RmiDistributedController controller = (RmiDistributedController) UnicastRemoteObject.exportObject(new DistributedController(null), 8888);
                final Registry registry = LocateRegistry.getRegistry();
                registry.bind(RmiDistributedController.class.getSimpleName(), controller);
                networkControllers.add(controller);
            }
        } catch (RemoteException | AlreadyBoundException | SocketException | NotBoundException e) {
            throw new Exception(e);
        }
    }

    @Override
    public void register(String host) throws RemoteException {
        final Registry registry = LocateRegistry.getRegistry(host);
        try {
            networkControllers.add((RmiDistributedController) registry.lookup(RmiDistributedController.class.getSimpleName()));
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unregister(String host) throws RemoteException {
        networkControllers.stream()
                .filter(rmiDistributedController -> {
                    try {
                        return rmiDistributedController.getHostName().equals(host);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                })
                .findAny()
                .ifPresent(networkControllers::remove);
    }

    @Override
    public Stream<RmiChair> getFreeChairs(RmiPhilosopherStatus philosopherStatus) throws RemoteException {
        return null;
    }

    @Override
    public RmiChair getNeighbourChair(RmiChair chair) throws RemoteException {
        return null;
    }

    @Override
    public RmiPhilosopherStatus getStatus() throws RemoteException {
        return null;
    }
}
