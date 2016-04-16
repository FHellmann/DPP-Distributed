package edu.hm.cs.vss;

import edu.hm.cs.vss.local.LocalMergedTable;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by Fabio Hellmann on 17.03.2016.
 */
public interface Table {
    int STATIC_PORT = 32984;

    default String getName() {
        return "127.0.0.1";
    }

    /**
     * @param tableHost
     */
    void addTable(final String tableHost);

    /**
     * @param tableHost
     */
    void removeTable(final String tableHost);

    /**
     * @return
     */
    Stream<Table> getTables();

    /**
     * Add a chair to the table.
     *
     * @param chair to add.
     */
    void addChair(final Chair chair);

    /**
     * @param chair
     */
    void removeChair(final Chair chair);

    /**
     * @return
     */
    default Stream<Chair> getChairs() {
        return getTables().flatMap(Table::getChairs);
    }

    /**
     * Get the neighbour chair of another chair. (If there is only one chair, then the same chair will be returned)
     *
     * @param chair to get the neighbour from.
     * @return the neighbour chair.
     */
    default Chair getNeighbourChair(final Chair chair) {
        final List<Chair> chairs = getChairs().collect(Collectors.toList());
        int indexOfChair = chairs.indexOf(chair);
        if (indexOfChair == 0) {
            indexOfChair = chairs.size();
        }
        return chairs.get(indexOfChair - 1); // Get the chair from the left hand side
    }

    /**
     * Set the table master for this table.
     *
     * @param tableMaster to set.
     */
    void setTableMaster(final TableMaster tableMaster);

    /**
     * Get the table master - never be <code>null</code>.
     *
     * @return the table master.
     */
    TableMaster getTableMaster();

    class Builder {
        private int amountChairs;
        private TableMaster tableMaster;

        public Builder withChairCount(final int amountOfChairs) {
            amountChairs = amountOfChairs;
            return this;
        }

        public Builder withTableMaster(TableMaster tableMaster) {
            this.tableMaster = tableMaster;
            return this;
        }

        public Table create() throws IOException {
            final Table table = new LocalMergedTable();
            table.setTableMaster(tableMaster);
            IntStream.rangeClosed(1, amountChairs - 1)
                    .mapToObj(index -> new Chair.Builder().create())
                    .forEach(table::addChair);
            return table;
        }
    }
}
