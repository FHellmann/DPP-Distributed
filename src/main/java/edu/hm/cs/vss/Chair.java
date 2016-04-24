package edu.hm.cs.vss;

import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Fabio Hellmann on 17.03.2016.
 */
public interface Chair extends Serializable {
    /**
     * Get a fork related to this seat.
     *
     * @return the fork.
     */
    Fork getFork();

    /**
     * @return <code>true</code> if the chair is available.
     */
    boolean isAvailable();

    /**
     * Blocks this chair immediately if it is available.
     *
     * @return the chair or <code>null</code> if the chair wasn't available.
     */
    Optional<Chair> blockIfAvailable() throws InterruptedException;

    /**
     * Set the chair available again.
     */
    void unblock();

    /**
     * Get the amount of waiting philosophers for this chair.
     *
     * @return the amount of waiting philosophers.
     */
    int getWaitingPhilosopherCount();

    class Builder implements Serializable {
        private static int counter = 1;
        private String name = "Chair-" + Integer.toString(counter++);

        public Builder setNameUniqueId() {
            name = "Chair-" + UUID.randomUUID().toString();
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Chair create() {
            return new Chair() {
                private final Fork fork = new Fork.Builder().withChair(this).setNameUniqueId().create();
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
                    if (semaphore.tryAcquire(1, TimeUnit.MINUTES) && block.compareAndSet(false, true)) {
                        return Optional.of(this);
                    }
                    semaphore.release();
                    return Optional.empty();
                }

                @Override
                public void unblock() {
                    block.set(false);
                    semaphore.release();
                }

                @Override
                public int getWaitingPhilosopherCount() {
                    return semaphore.getQueueLength() + (isAvailable() ? 0 : 1);
                }

                @Override
                public String toString() {
                    return name;
                }
            };
        }
    }
}
