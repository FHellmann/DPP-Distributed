package edu.hm.cs.vss;

import edu.hm.cs.vss.local.LocalPhilosopher;
import edu.hm.cs.vss.log.DummyLogger;
import edu.hm.cs.vss.log.FileLogger;
import edu.hm.cs.vss.log.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by Fabio Hellmann on 17.03.2016.
 */
public abstract class Philosopher extends Thread {
    protected static final int DEFAULT_EAT_ITERATIONS = 3;
    private static final long DEFAULT_TIME_TO_SLEEP = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MILLISECONDS);
    private static final long DEFAULT_TIME_TO_MEDIATE = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MILLISECONDS);
    private static final long DEFAULT_TIME_TO_EAT = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MILLISECONDS);
    protected static final long DEFAULT_TIME_TO_BANN = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MILLISECONDS);
    private static final int MAX_DEADLOCK_COUNT = 10;
    private static final DeadlockFunction DEADLOCK_FUNCTION = (philosopher, forks) -> {
        forks.parallelStream().forEach(Fork::unblock);
        forks.clear();
    };

    /**
     * Get the logger of the philosopher.
     *
     * @return the logger.
     */
    protected abstract Logger getLogger();

    /**
     * Get the table where the philosopher can get something to eat.
     *
     * @return the table.
     */
    protected abstract Table getTable();

    /**
     * Get the amount of eaten meals.
     *
     * @return the amount of eaten meals.
     */
    public abstract int getMealCount();

    /**
     * If a meal was eat increment the counter.
     */
    public abstract void incrementMealCount();

    /**
     * Get the iteration count of how many times the philosopher want's to eat something. (Default is 3)
     *
     * @return the iteration count.
     */
    protected abstract int getEatIterationCount();

    /**
     * Refuse the philosopher a seat at the table.
     */
    protected abstract void banned();

    /**
     * Allow the philosopher to sit down at the table.
     */
    protected abstract void unbanned();

    /**
     * Get the time the philosopher is no longer allowed to sit at the table.
     *
     * @return the time.
     */
    protected abstract Optional<Long> getBannedTime();

    /**
     * Get the time to sleep. (in Milliseconds)
     *
     * @return the time to sleep.
     */
    protected abstract long getTimeToSleep();

    /**
     * Get the time to eat. (in Milliseconds)
     *
     * @return the time to eat.
     */
    protected abstract long getTimeToEat();

    /**
     * Get the time to mediate. (in Milliseconds)
     *
     * @return the time to mediate.
     */
    protected abstract long getTimeToMediate();

    public abstract boolean isHungry();

    protected abstract Stream<Fork> getForks();

    public abstract void addOnStandUpListener(final OnStandUpListener listener);

    public abstract void removeOnStandUpListener(final OnStandUpListener listener);

    protected abstract Stream<OnStandUpListener> getOnStandUpListener();

    private Chair waitForSitDown() {
        say("Waiting for a nice seat...");

        Optional<Chair> chairOptional = Optional.empty();
        do {
            // waiting for a seat... if one is available it is directly blocked (removed from table)
            if (getTable().getTables().map(Table::getTableMaster).allMatch(tableMaster -> tableMaster.isAllowedToTakeSeat(getMealCount()))) {
                unbanned();

                // searching for the chair with a minimal queue size
                chairOptional = getTable().getTables()
                        .flatMap(Table::getChairs)
                        .min((chair1, chair2) -> Integer.compare(chair1.getWaitingPhilosopherCount(), chair2.getWaitingPhilosopherCount()));

                if (chairOptional.isPresent()) {
                    try {
                        chairOptional = chairOptional.get().blockIfAvailable();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                banned();

                getBannedTime().ifPresent(time -> {
                    say("I'm banned for " + time + " ms :'(");
                    try {
                        onThreadSleep(time);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } while (!chairOptional.isPresent());

        final Chair chair = chairOptional.get();
        say("Found a nice seat (" + chair.toString() + ")");

        return chair;
    }

    /**
     * Unblocks the seat and resets the philosophers seat.
     */
    private void standUp(final Chair chair) {
        releaseForks();
        say("Stand up from seat (" + chair.toString() + ")");
        chair.unblock();
    }

    protected Stream<Fork> waitForForks(final Chair chair) {
        say("Waiting for 2 forks...");

        List<Fork> foundForks = new ArrayList<>();

        final Fork fork = chair.getFork();
        final Fork neighbourFork = getTable().getNeighbourChair(chair).getFork();

        while (foundForks.size() < 2) {
            // Reset all
            int deadlockDetectionCount = 0;

            // Try to get the right fork
            while (foundForks.size() < 1) {
                fork.blockIfAvailable().ifPresent(foundForks::add);

                if (foundForks.size() < 1 && deadlockDetectionCount++ > MAX_DEADLOCK_COUNT) {
                    DEADLOCK_FUNCTION.onDeadlockDetected(this, foundForks);
                    break;
                }
            }

            if (foundForks.size() < 1) {
                // First fork was not found -> skip to start
                continue;
            }

            // Reset only the deadlock counter
            deadlockDetectionCount = 0;

            // Try to get the left fork
            while (foundForks.size() < 2) {
                neighbourFork.blockIfAvailable().ifPresent(foundForks::add);

                if (foundForks.size() < 2 && deadlockDetectionCount++ > MAX_DEADLOCK_COUNT) {
                    DEADLOCK_FUNCTION.onDeadlockDetected(this, foundForks);
                    break;
                }
            }
        }

        say("Found 2 forks (" + fork.toString() + ", " + neighbourFork.toString() + ")! :D");

        return foundForks.stream();
    }

    /**
     * Unblock all forks and reset the forks the philosopher holds.
     */
    protected void releaseForks() {
        final String forks = getForks().map(Object::toString).collect(Collectors.joining(", "));
        say("Release my forks" + ((forks.length() > 0) ? " (" + forks + ")" : " (no forks picked yet)"));
        getForks().forEach(Fork::unblock);
    }

    /**
     * The philosopher is eating.
     */
    private void eat() throws InterruptedException {
        incrementMealCount();
        getOnStandUpListener().forEach(listener -> listener.onStandUp(this));
        say("Eating for " + getTimeToEat() + " ms");
        onThreadSleep(getTimeToEat());
    }

    /**
     * The philosopher is mediating.
     */
    private void mediate() throws InterruptedException {
        say("Mediating for " + getTimeToMediate() + " ms");
        onThreadSleep(getTimeToMediate());
    }

    /**
     * The philosopher is sleeping.
     */
    private void sleep() throws InterruptedException {
        say("Sleeping for " + getTimeToSleep() + " ms");
        onThreadSleep(getTimeToSleep());
    }

    /**
     * What the philosopher do in his life...
     */
    @Override
    public void run() {
        say("I'm alive!");

        getTable().getTableMaster().register(this);

        try {
            while (!isInterrupted()) {
                // 3 Iterations by default... or more if the philosopher is very hungry
                IntStream.rangeClosed(0, getEatIterationCount() - 1)
                        .mapToObj(index -> waitForSitDown()) // Sit down on a free chair -> waiting for a free
                        .peek(this::waitForForks) // Grab two forks -> waiting for two free
                        .peek(tmp -> {
                            try {
                                eat();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }) // Eat the next portion
                        .peek(this::standUp) // Stand up from chair and release forks
                        .forEach(tmp -> {
                            try {
                                mediate();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }); // Go to mediate

                // Sleep
                sleep();
            }
        } catch (Exception e) {
            // just for leaving the while loop
        } finally {
            getTable().getTableMaster().unregister(this);
        }
    }

    private void say(final String message) {
        getLogger().log("[" + getName() + "; Meals=" + getMealCount() + "]: " + message);
    }

    private void onThreadSleep(final long time) throws InterruptedException {
        Thread.sleep(time);
    }

    @FunctionalInterface
    public interface OnStandUpListener {
        void onStandUp(final Philosopher philosopher);
    }

    @FunctionalInterface
    private interface DeadlockFunction {
        void onDeadlockDetected(final Philosopher philosopher, final List<Fork> forkStream);
    }

    public static class Builder {
        private static int count = 1;
        private String namePrefix = "";
        private String nameSuffix = "";
        private String name = "Philosopher-";
        private Logger logger = new DummyLogger();
        private Table table;
        private long timeSleep = DEFAULT_TIME_TO_SLEEP;
        private long timeEat = DEFAULT_TIME_TO_EAT;
        private long timeMediate = DEFAULT_TIME_TO_MEDIATE;
        private boolean hungry;
        private int takenMeals = 0;

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder setUniqueName() {
            this.nameSuffix = UUID.randomUUID().toString();
            return this;
        }

        public Builder setIdName() {
            this.nameSuffix = Integer.toString(count++);
            return this;
        }

        public Builder setTable(final Table table) {
            this.table = table;
            return this;
        }

        public Builder setFileLogger() {
            return setLogger(new FileLogger(name + nameSuffix));
        }

        public Builder setLogger(final Logger logger) {
            this.logger = logger;
            return this;
        }

        public Builder setTimeToSleep(final long timeToSleep) {
            this.timeSleep = timeToSleep;
            return this;
        }

        public Builder setTimeToEat(final long timeToEat) {
            this.timeEat = timeToEat;
            return this;
        }

        public Builder setTimeToMediate(final long timeToMediate) {
            this.timeMediate = timeToMediate;
            return this;
        }

        public Builder setHungry(final boolean hungry) {
            this.hungry = hungry;
            if (hungry) {
                this.namePrefix = "Hungry-";
            }
            return this;
        }

        public Builder setTakenMeals(int takenMeals) {
            this.takenMeals = takenMeals;
            return this;
        }

        public Philosopher create() {
            if (table == null) {
                throw new NullPointerException("Table can not be null. Use new Philosopher.Builder().setTable(Table).[...].create()");
            }
            final LocalPhilosopher philosopher = new LocalPhilosopher(namePrefix + name + nameSuffix, logger, table, timeSleep, timeEat, timeMediate, hungry);
            IntStream.rangeClosed(1, takenMeals)
                    .forEach(index -> philosopher.incrementMealCount());
            return philosopher;
        }
    }
}
