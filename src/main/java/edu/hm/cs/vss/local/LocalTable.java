package edu.hm.cs.vss.local;

import edu.hm.cs.vss.*;
import edu.hm.cs.vss.log.DummyLogger;
import edu.hm.cs.vss.log.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by Fabio Hellmann on 14.04.2016.
 */
public class LocalTable implements Table {
    private final List<Chair> chairs = Collections.synchronizedList(new LinkedList<>());
    private final Logger logger;
    private TableMaster tableMaster;

    public LocalTable() throws IOException {
        this(new DummyLogger());
    }

    public LocalTable(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void connectToTable(String tableHost) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disconnectFromTable(String tableHost) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Table> getTables() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addPhilosopher(Philosopher philosopher) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removePhilosopher(Philosopher philosopher) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Philosopher> getPhilosophers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addChair(Chair chair) {
        chairs.add(chair);
    }

    @Override
    public void removeChair(Chair chair) {
        chairs.remove(chair);
    }

    @Override
    public Stream<Chair> getChairs() {
        return chairs.stream();
    }

    @Override
    public TableMaster getTableMaster() {
        return tableMaster;
    }

    @Override
    public void setTableMaster(TableMaster tableMaster) {
        if (tableMaster != null) {
            this.tableMaster = tableMaster;
        }
    }

    @Override
    public BackupService getBackupService() {
        throw new UnsupportedOperationException();
    }
}
