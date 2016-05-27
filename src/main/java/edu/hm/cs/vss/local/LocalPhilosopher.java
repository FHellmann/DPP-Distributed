package edu.hm.cs.vss.local;

import edu.hm.cs.vss.Chair;
import edu.hm.cs.vss.Fork;
import edu.hm.cs.vss.Philosopher;
import edu.hm.cs.vss.Table;
import edu.hm.cs.vss.log.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Fabio Hellmann on 17.03.2016.
 */
public class LocalPhilosopher extends Philosopher {
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

    public LocalPhilosopher(final String name,
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

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public Table getTable() {
        return table;
    }

    @Override
    public int getMealCount() {
        return mealCount;
    }

    @Override
    public void incrementMealCount() {
        mealCount++;
    }

    @Override
    public int getEatIterationCount() {
        return eatIterations;
    }

    @Override
    public void banned() {
        bannedTime = DEFAULT_TIME_TO_BANN;
    }

    @Override
    public void unbanned() {
        bannedTime = -1;
    }

    @Override
    public Optional<Long> getBannedTime() {
        if (bannedTime >= 0) {
            return Optional.ofNullable(bannedTime);
        }
        return Optional.empty();
    }

    @Override
    public long getTimeToSleep() {
        return timeSleep;
    }

    @Override
    public long getTimeToEat() {
        return timeEat;
    }

    @Override
    public long getTimeToMediate() {
        return timeMediate;
    }

    public boolean isHungry() {
        return veryHungry;
    }

    @Override
    public Stream<Fork> waitForForks(Chair chair) {
        this.forks = super.waitForForks(chair).collect(Collectors.toList());
        return forks.stream();
    }

    @Override
    public void releaseForks() {
        super.releaseForks();
        forks.clear();
    }

    @Override
    public Stream<Fork> getForks() {
        return forks.stream();
    }

    @Override
    public void addOnStandUpListener(OnStandUpListener listener) {
        onStandUpListeners.add(listener);
    }

    @Override
    public void removeOnStandUpListener(OnStandUpListener listener) {
        onStandUpListeners.remove(listener);
    }

    @Override
    public Stream<OnStandUpListener> getOnStandUpListener() {
        return onStandUpListeners.stream();
    }
}
