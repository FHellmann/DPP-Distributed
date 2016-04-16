package edu.hm.cs.vss.local;

import edu.hm.cs.vss.Chair;
import edu.hm.cs.vss.Table;
import edu.hm.cs.vss.TableMaster;
import edu.hm.cs.vss.remote.RemoteTable;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by Fabio on 16.04.2016.
 */
public class LocalMergedTable implements Table {
    private final List<Table> tables = Collections.synchronizedList(new LinkedList<>());

    public LocalMergedTable() throws IOException {
        tables.add(new LocalTable());
    }

    @Override
    public void addTable(String tableHost) {
        if (!tables.parallelStream().anyMatch(table -> table.getName().equals(tableHost))) {
            try {
                final Table table = new RemoteTable(tableHost);
                tables.stream()
                        .map(Table::getName)
                        .forEach(table::addTable);
                tables.add(table);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void removeTable(String tableHost) {
        if (tables.parallelStream().anyMatch(table -> table.getName().equals(tableHost))) {
            tables.parallelStream()
                    .filter(table -> !table.equals(this))
                    .peek(table -> table.removeTable(tableHost))
                    .filter(table -> table.getName().equals(tableHost))
                    .findAny()
                    .ifPresent(tables::remove);
        }
    }

    @Override
    public Stream<Table> getTables() {
        return tables.stream();
    }

    @Override
    public void addChair(Chair chair) {
        getLocalTable().addChair(chair);
    }

    @Override
    public void removeChair(Chair chair) {
        getLocalTable().removeChair(chair);
    }

    @Override
    public void setTableMaster(TableMaster tableMaster) {
        getLocalTable().setTableMaster(tableMaster);
    }

    @Override
    public TableMaster getTableMaster() {
        return getLocalTable().getTableMaster();
    }

    private Table getLocalTable() {
        return tables.parallelStream().filter(table -> table instanceof LocalTable).findFirst().get();
    }
}
