package edu.hm.cs.vss.local;

import edu.hm.cs.vss.Chair;
import edu.hm.cs.vss.Fork;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Fabio Hellmann on 14.04.2016.
 */
public class LocalChair implements Chair {
    private final String name = "Chair-" + UUID.randomUUID().toString();
    private final Fork fork = new Fork.Builder().withChair(this).create();
    private final AtomicBoolean block = new AtomicBoolean(false);
    private final Semaphore semaphore = new Semaphore(1, true);

    @Override
    public Fork getFork() {
        return fork;
    }

    @Override
    public boolean isAvailable() {
        return !block.get();
    }

    @Override
    public Optional<Chair> blockIfAvailable() throws InterruptedException {
        semaphore.acquire();
        if (block.compareAndSet(false, true)) {
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public void unblock() {
        block.set(false);
        semaphore.release();
    }

    @Override
    public int getQueueSize() {
        return semaphore.getQueueLength() + (isAvailable() ? 0 : 1);
    }

    @Override
    public String toString() {
        return name;
    }
}
