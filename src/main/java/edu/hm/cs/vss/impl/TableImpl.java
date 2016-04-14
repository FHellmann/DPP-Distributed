package edu.hm.cs.vss.impl;

import edu.hm.cs.vss.*;

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
 * Created by Fabio Hellmann on 17.03.2016.
 */
public class TableImpl implements Table {
    private static final TableMaster DEFAULT_TABLE_MASTER = philosopher -> true;
    private final List<Chair> chairs = Collections.synchronizedList(new ArrayList<>());
    private TableMaster tableMaster = DEFAULT_TABLE_MASTER;

    @Override
    public void addChair(Chair chair) {
        chairs.add(chair);
    }

    @Override
    public void removeChair(Chair chair) {
        if (chair.isAvailable()) {
            chairs.remove(chair);
        }
    }

    @Override
    public Stream<Chair> getChairs() {
        return chairs.parallelStream();
    }

    @Override
    public Stream<Chair> getFreeChairs(final Philosopher philosopher) throws InterruptedException {
        if (!getTableMaster().isAllowedToTakeSeat(philosopher)) {
            return Stream.empty();
        }
        return chairs.parallelStream().filter(Chair::isAvailable);
    }

    @Override
    public Chair getNeighbourChair(final Chair chair) {
        int indexOfChair = chairs.indexOf(chair);
        if (indexOfChair == 0) {
            indexOfChair = chairs.size();
        }
        return chairs.get(indexOfChair - 1); // Get the chair from the left hand side
    }

    @Override
    public void setTableMaster(TableMaster tableMaster) {
        if (tableMaster == null) {
            this.tableMaster = DEFAULT_TABLE_MASTER;
        } else {
            this.tableMaster = tableMaster;
        }
    }

    @Override
    public TableMaster getTableMaster() {
        return tableMaster;
    }
}
