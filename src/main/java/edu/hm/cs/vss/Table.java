package edu.hm.cs.vss;

import edu.hm.cs.vss.local.LocalTable;
import edu.hm.cs.vss.local.LocalTablePool;
import edu.hm.cs.vss.log.DummyLogger;
import edu.hm.cs.vss.log.FileLogger;
import edu.hm.cs.vss.log.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by Fabio Hellmann on 17.03.2016.
 */
public interface Table {
    int NETWORK_PORT = 32984;

    /**
     * 
     * @return
     */
    default String getName() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "127.0.0.1"; // TODO This is a potential bug if the host can not be detected automaticly
        }
    }

    /**
     * @param tableHost
     */
    void connectToTable(final String tableHost);

    /**
     * @param tableHost
     */
    void disconnectFromTable(final String tableHost);

    /**
     * @return
     */
    Stream<Table> getTables();

    /**
     *
     * @param philosopher
     * @return
     */
    Philosopher addPhilosopher(final Philosopher philosopher);

    /**
     *
     * @param philosopher
     */
    void removePhilosopher(final Philosopher philosopher);

    /**
     *
     * @return
     */
    Stream<Philosopher> getPhilosophers();

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
    Stream<Chair> getChairs();

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

    /**
     * Get the backup service.
     *
     * @return the backup service.
     */
    BackupService getBackupService();

    class Builder {
        private int amountChairs;
        private TableMaster tableMaster = philosopher -> true;
        private Logger logger = new DummyLogger();

        public Builder withChairCount(final int amountOfChairs) {
            amountChairs = amountOfChairs;
            return this;
        }

        public Builder withTableMaster(TableMaster tableMaster) {
            this.tableMaster = tableMaster;
            return this;
        }

        public Builder setFileLogger() {
            this.logger = new FileLogger("tables");
            return this;
        }

        public Table createLocal() {
            final Table table = new LocalTable(logger);
            table.setTableMaster(tableMaster);
            IntStream.rangeClosed(1, amountChairs - 1)
                    .mapToObj(index -> new Chair.Builder().setNameUniqueId().create())
                    .forEach(table::addChair);
            return table;
        }

        public Table createNetwork() throws IOException {
            final Table table = new LocalTablePool(logger);
            table.setTableMaster(tableMaster);
            IntStream.rangeClosed(1, amountChairs - 1)
                    .mapToObj(index -> new Chair.Builder().setNameUniqueId().create())
                    .forEach(table::addChair);
            return table;
        }
    }
}
