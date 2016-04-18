package edu.hm.cs.vss;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The Backup Service is responsible for creating, storing and restoring backups.
 */
public interface BackupService {
    /**
     * Creates a new Backup Service instance with a basic backup store.
     *
     * @param table the backup service is responsible for.
     * @return the backup service.
     */
    static BackupService create(final Table table) {
        return new BackupService() {
            private final ConcurrentMap<String, Backup> hostBackupMap = new ConcurrentHashMap<>();

            @Override
            public Backup createBackup() {
                final String host = table.getName();
                final int cpuCores = Runtime.getRuntime().availableProcessors();
                final int chairCount = (int) table.getChairs().count();
                final List<String> philosopherNames = table.getPhilosophers().map(Philosopher::getName)
                        .collect(Collectors.toList());
                final List<Integer> philosopherMealCount = table.getPhilosophers().map(Philosopher::getMealCount)
                        .collect(Collectors.toList());
                final List<Boolean> philosopherHungry = table.getPhilosophers().map(Philosopher::isHungry)
                        .collect(Collectors.toList());
                return new Backup() {
                    @Override
                    public String getHost() {
                        return host;
                    }

                    @Override
                    public int getCores() {
                        return cpuCores;
                    }

                    @Override
                    public int getChairCount() {
                        return chairCount;
                    }

                    @Override
                    public int getPhilosopherCount() {
                        return philosopherNames.size();
                    }

                    @Override
                    public String getPhilosopherName(int index) {
                        return philosopherNames.get(index);
                    }

                    @Override
                    public int getPhilosopherMealCount(int index) {
                        return philosopherMealCount.get(index);
                    }

                    @Override
                    public boolean isPhilosopherHungry(int index) {
                        return philosopherHungry.get(index);
                    }
                };
            }

            @Override
            public void storeBackup(Backup backup) {
                hostBackupMap.put(backup.getHost(), backup);
            }

            @Override
            public void restoreBackup(String host) {
                final Backup backup = hostBackupMap.get(host);

                IntStream.rangeClosed(1, backup.getChairCount())
                        .mapToObj(index -> new Chair.Builder().setNameUniqueId().create())
                        .forEach(table::addChair);

                IntStream.rangeClosed(0, backup.getPhilosopherCount() - 1)
                        .mapToObj(index -> new Philosopher.Builder()
                                .setTable(table)
                                .name(backup.getPhilosopherName(index))
                                .setTakenMeals(backup.getPhilosopherMealCount(index))
                                .setHungry(backup.isPhilosopherHungry(index))
                                .setFileLogger()
                                .create())
                        .forEach(table::addPhilosopher);
            }

            @Override
            public void deleteBackup(String host) {
                hostBackupMap.remove(host);
            }
        };
    }

    /**
     * Creates a snapshot from this tables and philosophers settings.
     *
     * @return the backup.
     */
    Backup createBackup();

    /**
     * Safes a backup.
     *
     * @param backup to safe.
     */
    void storeBackup(final Backup backup);

    /**
     * Restores a backup from the given host.
     *
     * @param host to restore the backup from.
     */
    void restoreBackup(final String host);

    /**
     * Delete a backup from the given host.
     *
     * @param host to delete the backup from.
     */
    void deleteBackup(final String host);
}
