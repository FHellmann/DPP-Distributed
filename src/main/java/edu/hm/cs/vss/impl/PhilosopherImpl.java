package edu.hm.cs.vss.impl;

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
public class PhilosopherImpl implements Philosopher {
    private final String name;
    private Logger logger;
    private final Table table;
    private final long timeSleep;
    private final long timeEat;
    private final long timeMediate;
    private Chair chair;
    private List<Fork> forks = new ArrayList<>();
    private int eatIterations;
    private int mealCount;
    private long bannedTime = -1;
    private OnStandUpListener onStandUpListener;

    public PhilosopherImpl(final String name,
                           final Logger logger,
                           final Table table,
                           final long timeSleep,
                           final long timeEat,
                           final long timeMediate,
                           final boolean veryHungry) {
        this(name, logger, table, timeSleep, timeEat, veryHungry ? timeMediate / 2 : timeMediate, veryHungry ? DEFAULT_EAT_ITERATIONS * 2 : DEFAULT_EAT_ITERATIONS);
    }

    private PhilosopherImpl(final String name,
                           final Logger logger,
                           final Table table,
                           final long timeSleep,
                           final long timeEat,
                           final long timeMediate,
                           final int eatIterations) {
        this.name = name;
        this.logger = logger;
        this.table = table;
        this.timeSleep = timeSleep;
        this.timeEat = timeEat;
        this.timeMediate = timeMediate;
        this.eatIterations = eatIterations;
    }

    @Override
    public String getName() {
        return name;
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

    @Override
    public Chair waitForSitDown() {
        this.chair = Philosopher.super.waitForSitDown();
        return chair;
    }

    @Override
    public void standUp() {
        Philosopher.super.standUp();
        chair = null;
    }

    @Override
    public Optional<Chair> getChair() {
        return Optional.ofNullable(chair);
    }

    @Override
    public Stream<Fork> waitForForks(Chair chair) {
        this.forks = Philosopher.super.waitForForks(chair).collect(Collectors.toList());
        return forks.stream();
    }

    @Override
    public void releaseForks() {
        Philosopher.super.releaseForks();
        forks.clear();
    }

    @Override
    public Stream<Fork> getForks() {
        return forks.stream();
    }

    @Override
    public void setOnStandUpListener(OnStandUpListener listener) {
        onStandUpListener = listener;
    }

    @Override
    public Optional<OnStandUpListener> getOnStandUpListener() {
        return Optional.ofNullable(onStandUpListener);
    }
}
