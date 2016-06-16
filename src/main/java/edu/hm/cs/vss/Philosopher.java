package edu.hm.cs.vss;

import edu.hm.cs.vss.log.DummyLogger;
import edu.hm.cs.vss.log.FileLogger;
import edu.hm.cs.vss.log.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by Fabio Hellmann on 17.03.2016.
 */
public class Philosopher extends Thread {
    protected static final int DEFAULT_EAT_ITERATIONS = 3;
    private static final long DEFAULT_TIME_TO_SLEEP = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MILLISECONDS);
    private static final long DEFAULT_TIME_TO_MEDIATE = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MILLISECONDS);
    private static final long DEFAULT_TIME_TO_EAT = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MILLISECONDS);
    protected static final long DEFAULT_TIME_TO_BANN = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MILLISECONDS);
    private static final int MAX_DEADLOCK_COUNT = 10;
    private static final DeadlockFunction DEADLOCK_FUNCTION = (philosopher, forks) -> {
        //philosopher.say("I deadlocked, unlocking forks");
        forks.parallelStream().forEach(Fork::unblock);
        forks.clear();
    };
    private AtomicBoolean threadSuspended = new AtomicBoolean(false);

    private final static Object wakeSync = new Object();
    private final Logger logger;
    private final Table table;
    private final long timeSleep;
    private final long timeEat;
    private final long timeMediate;
    private final boolean veryHungry;
    private List<Fork> forks = new ArrayList<>();
    private final int eatIterations;
    private int mealCount;
    private long bannedTime = -1;
    private final List<OnStandUpListener> onStandUpListeners = new ArrayList<>();

    public Philosopher(final String name,
                       final Logger logger,
                       final Table table,
                       final long timeSleep,
                       final long timeEat,
                       final long timeMediate,
                       final boolean veryHungry) {
        setName(name);
        this.logger = logger;
        this.table = table;
        this.timeSleep = timeSleep;
        this.timeEat = timeEat;
        this.timeMediate = veryHungry ? timeMediate / 2 : timeMediate;
        this.veryHungry = veryHungry;
        this.eatIterations = veryHungry ? DEFAULT_EAT_ITERATIONS * 2 : DEFAULT_EAT_ITERATIONS;
    }

    /**
     * Get the logger of the philosopher.
     *
     * @return the logger.
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Get the table where the philosopher can get something to eat.
     *
     * @return the table.
     */
    public Table getTable() {
        return table;
    }

    /**
     * Get the amount of eaten meals.
     *
     * @return the amount of eaten meals.
     */
    public int getMealCount() {
        return mealCount;
    }

    /**
     * If a meal was eat increment the counter.
     */
    public void incrementMealCount() {
        mealCount++;
    }

    /**
     * Get the iteration count of how many times the philosopher want's to eat something. (Default is 3)
     *
     * @return the iteration count.
     */
    public int getEatIterationCount() {
        return eatIterations;
    }

    /**
     * Refuse the philosopher a seat at the table.
     */
    private void banned() {
        bannedTime = DEFAULT_TIME_TO_BANN;
    }

    /**
     * Allow the philosopher to sit down at the table.
     */
    private void unbanned() {
        bannedTime = -1;
    }

    /**
     * Get the time the philosopher is no longer allowed to sit at the table.
     *
     * @return the time.
     */
    private Optional<Long> getBannedTime() {
        if (bannedTime >= 0) {
            return Optional.ofNullable(bannedTime);
        }
        return Optional.empty();
    }

    /**
     * Get the time to sleep. (in Milliseconds)
     *
     * @return the time to sleep.
     */
    public long getTimeToSleep() {
        return timeSleep;
    }

    /**
     * Get the time to eat. (in Milliseconds)
     *
     * @return the time to eat.
     */
    public long getTimeToEat() {
        return timeEat;
    }

    /**
     * Get the time to mediate. (in Milliseconds)
     *
     * @return the time to mediate.
     */
    public long getTimeToMediate() {
        return timeMediate;
    }

    public boolean isHungry() {
        return veryHungry;
    }

    private Stream<Fork> getForks() {
        return forks.stream();
    }

    public void addOnStandUpListener(OnStandUpListener listener) {
        onStandUpListeners.add(listener);
    }

    public void removeOnStandUpListener(OnStandUpListener listener) {
        onStandUpListeners.remove(listener);
    }

    private Stream<OnStandUpListener> getOnStandUpListener() {
        return onStandUpListeners.stream();
    }

    private Chair waitForSitDown() {
        Optional<Chair> chairOptional = Optional.empty();
        say("Waiting for a nice seat...");

        do {
            say("Waiting for seat");

            // waiting for a seat... if one is available it is directly blocked (removed from table)
            if (getTable().getTableMaster().isAllowedToTakeSeat(getMealCount())) {
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
        onStandUpListeners.stream().forEach(listener -> listener.onStandUp(this));
    }

    private Stream<Fork> waitForForks(final Chair chair) {
        say("Waiting for 2 forks...");

        List<Fork> foundForks = new ArrayList<>();

        final Fork fork = chair.getFork();
        final Fork neighbourFork = getTable().getNeighbourChair(chair).getFork();

        while (foundForks.size() < 2) {
            // Reset all
            int deadlockDetectionCount = 0;

            // Try to get the right fork
            while (foundForks.size() < 1) {
                if (isInterrupted()) {
                    // Leave this never ending loop if the thread get's interrupted
                    throw new RuntimeException();
                }

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
                if (isInterrupted()) {
                    // Leave this never ending loop if the thread get's interrupted
                    throw new RuntimeException();
                }

                neighbourFork.blockIfAvailable().ifPresent(foundForks::add);

                if (foundForks.size() < 2 && deadlockDetectionCount++ > MAX_DEADLOCK_COUNT) {
                    DEADLOCK_FUNCTION.onDeadlockDetected(this, foundForks);
                    break;
                }
            }
        }

        say("Found 2 forks (" + fork.toString() + ", " + neighbourFork.toString() + ")! :D");

        this.forks = foundForks;
        return forks.stream();
    }

    /**
     * Unblock all forks and reset the forks the philosopher holds.
     */
    private void releaseForks() {
        final String forksText = forks.stream().map(Object::toString).collect(Collectors.joining(", "));
        say("Release my forks" + ((forksText.length() > 0) ? " (" + forksText + ")" : " (no forks picked yet)"));
        forks.forEach(Fork::unblock);
        forks.clear();
    }

    /**
     * The philosopher is eating.
     */
    private void eat() throws InterruptedException {
        incrementMealCount();
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
            final Philosopher philosopher = new Philosopher(namePrefix + name + nameSuffix, logger, table, timeSleep, timeEat, timeMediate, hungry);
            IntStream.rangeClosed(1, takenMeals)
                    .forEach(index -> philosopher.incrementMealCount());
            return philosopher;
        }
    }
}
