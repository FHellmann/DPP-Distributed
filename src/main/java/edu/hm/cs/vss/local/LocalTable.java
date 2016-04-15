package edu.hm.cs.vss.local;

import edu.hm.cs.vss.Chair;
import edu.hm.cs.vss.Table;
import edu.hm.cs.vss.TableMaster;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by Fabio Hellmann on 14.04.2016.
 */
public class LocalTable implements Table {
    private static final TableMaster DEFAULT_TABLE_MASTER = philosopher -> true;
    private final List<Table> tables = Collections.synchronizedList(new LinkedList<>());
    private final List<Chair> chairs = Collections.synchronizedList(new ArrayList<>());
    private TableMaster tableMaster = DEFAULT_TABLE_MASTER;

    public LocalTable() throws IOException {
        tables.add(this);
    }

    @Override
    public void addTable(final String tableHost) {
        new Builder().connectTo(tableHost).create().ifPresent(table -> {
            tables.stream()
                    .map(Table::getName)
                    .forEach(table::addTable);
            tables.add(table);
        });
    }

    @Override
    public void removeTable(final String tableHost) {
        tables.parallelStream()
                .filter(table -> !table.equals(this))
                .peek(table -> table.removeTable(tableHost))
                .filter(table -> table.getName().equals(tableHost))
                .findAny()
                .ifPresent(tables::remove);
    }

    @Override
    public Stream<Table> getTables() {
        return tables.stream();
    }

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
        return chairs.stream();
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
