package edu.hm.cs.vss;

import edu.hm.cs.vss.impl.TableImpl;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by Fabio Hellmann on 17.03.2016.
 */
public interface Table {
    /**
     * Add a chair to the table.
     *
     * @param chair to add.
     */
    void addChair(final Chair chair);

    /**
     *
     * @param chair
     */
    void removeChair(final Chair chair);

    /**
     *
     * @return
     */
    Stream<Chair> getChairs();

    /**
     * Get the next free chair or null. If a free chair was found, this chair will be automatically blocked.
     *
     * @param philosopher who wants a seat.
     * @return the next free chair or null.
     * @throws InterruptedException
     */
    Stream<Chair> getFreeChairs(final Philosopher philosopher) throws InterruptedException;

    /**
     * Get the neighbour chair of another chair. (If there is only one chair, then the same chair will be returned)
     *
     * @param chair to get the neighbour from.
     * @return the neighbour chair.
     */
    Chair getNeighbourChair(final Chair chair);

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

        public Table create() {
            final TableImpl table = new TableImpl();
            table.setTableMaster(tableMaster);
            IntStream.rangeClosed(1, amountChairs - 1)
                    .mapToObj(index -> new Chair.Builder().create())
                    .forEach(table::addChair);
            return table;
        }
    }
}
