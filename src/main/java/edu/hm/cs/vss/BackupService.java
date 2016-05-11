package edu.hm.cs.vss;

import edu.hm.cs.vss.remote.RemoteChair;
import edu.hm.cs.vss.remote.RemoteTable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * The Backup Service is responsible for creating, storing and restoring backups.
 */
public interface BackupService extends Serializable {
    /**
     * Creates a new Backup Service instance with a basic backup store.
     *
     * @return the backup service.
     */
    static BackupService create(final RemoteTable table) {
        return new BackupService() {
            private final List<Chair> chairList = Collections.synchronizedList(new ArrayList<>());
            private final List<Philosopher> philosopherList = Collections.synchronizedList(new ArrayList<>());

            @Override
            public void addChair(Chair chair) {
                chairList.add(new RemoteChair(chair, table));
            }

            @Override
            public void removeChair(Chair chair) {
                chairList.remove(chair);
            }

            @Override
            public Stream<Chair> getChairs() {
                return chairList.stream();
            }

            @Override
            public void addPhilosopher(Philosopher philosopher) {
                philosopherList.add(philosopher);
            }

            @Override
            public void removePhilosopher(Philosopher philosopher) {
                philosopherList.remove(philosopher);
            }

            @Override
            public Stream<Philosopher> getPhilosophers() {
                return philosopherList.stream();
            }
        };
    }

    default void addChair(final String name) {
        addChair(new Chair.Builder().setName(name).create());
    }

    void addChair(final Chair chair);

    default void removeChair(final String name) {
        getChairs().parallel()
                .filter(chair -> chair.toString().equals(name))
                .findAny()
                .ifPresent(this::removeChair);
    }

    void removeChair(final Chair chair);

    Stream<Chair> getChairs();

    default void addPhilosopher(final String name, final boolean hungry) {
        addPhilosopher(new Philosopher.Builder().name(name).setHungry(hungry).setFileLogger().create());
    }

    void addPhilosopher(final Philosopher philosopher);

    default void removePhilosopher(final String name) {
        getPhilosophers().parallel()
                .filter(philosopher -> philosopher.getName().equals(name))
                .findAny()
                .ifPresent(this::removePhilosopher);
    }

    void removePhilosopher(final Philosopher philosopher);

    Stream<Philosopher> getPhilosophers();

    default void onPhilosopherStandUp(final String name) {
        getPhilosophers().parallel()
                .filter(philosopher -> philosopher.getName().equals(name))
                .findAny()
                .ifPresent(Philosopher::incrementMealCount);
    }
}
