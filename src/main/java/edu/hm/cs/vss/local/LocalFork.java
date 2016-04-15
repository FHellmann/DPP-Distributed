package edu.hm.cs.vss.local;

import edu.hm.cs.vss.Chair;
import edu.hm.cs.vss.Fork;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Fabio Hellmann on 14.04.2016.
 */
public class LocalFork implements Fork {
    private final String name;
    private final AtomicBoolean block = new AtomicBoolean(false);

    public LocalFork(final Chair chair) {
        name = "Fork-" + UUID.randomUUID().toString() + " from " + chair.toString();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean isAvailable() {
        return !block.get();
    }

    @Override
    public Optional<Fork> blockIfAvailable() {
        if (block.compareAndSet(false, true)) {
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public void unblock() {
        block.set(false);
    }
}
