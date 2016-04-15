package edu.hm.cs.vss.remote;

import edu.hm.cs.vss.Chair;
import edu.hm.cs.vss.Fork;
import edu.hm.cs.vss.Philosopher;
import edu.hm.cs.vss.Table;
import edu.hm.cs.vss.log.Logger;

import java.net.Socket;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created by Fabio Hellmann on 14.04.2016.
 */
public class RemotePhilosopher implements Philosopher {
    private final Socket socket;

    public RemotePhilosopher(final Socket socket) {
        this.socket = socket;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Logger getLogger() {
        return null;
    }

    @Override
    public Table getTable() {
        return null;
    }

    @Override
    public void kill() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getMealCount() {
        return 0;
    }

    @Override
    public void incrementMealCount() {

    }

    @Override
    public int getEatIterationCount() {
        return 0;
    }

    @Override
    public void banned() {

    }

    @Override
    public void unbanned() {

    }

    @Override
    public Optional<Long> getBannedTime() {
        return null;
    }

    @Override
    public long getTimeToSleep() {
        return 0;
    }

    @Override
    public long getTimeToEat() {
        return 0;
    }

    @Override
    public long getTimeToMediate() {
        return 0;
    }

    @Override
    public Optional<Chair> getChair() {
        return null;
    }

    @Override
    public Stream<Fork> getForks() {
        return null;
    }

    @Override
    public void setOnStandUpListener(OnStandUpListener listener) {

    }

    @Override
    public Optional<OnStandUpListener> getOnStandUpListener() {
        return null;
    }

    @Override
    public Chair waitForSitDown() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Fork> waitForForks(Chair chair) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException();
    }
}
