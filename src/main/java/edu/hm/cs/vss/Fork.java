package edu.hm.cs.vss;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Fabio Hellmann on 17.03.2016.
 */
public interface Fork {

    /**
     * @return <code>true</code> if the fork is available.
     */
    boolean isAvailable();

    /**
     * Blocks this fork immediately if it is available.
     *
     * @return the fork or <code>null</code> if the fork wasn't available.
     */
    Optional<Fork> blockIfAvailable();

    /**
     * Set the fork available again.
     */
    void unblock();

    class Builder {
        private Chair chair;

        public Builder withChair(final Chair chair) {
            this.chair = chair;
            return this;
        }

        public Fork create() {
            return new Fork() {
                private final String name = "Fork-" + UUID.randomUUID().toString();
                private final AtomicBoolean block = new AtomicBoolean(false);

                @Override
                public String toString() {
                    return name + " from " + chair.toString();
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
            };
        }
    }
}
