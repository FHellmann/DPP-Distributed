package edu.hm.cs.vss;

import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Fabio Hellmann on 17.03.2016.
 */
public interface Fork extends Serializable {

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

    class Builder implements Serializable {
        private Chair chair;

        public Builder withChair(final Chair chair) {
            this.chair = chair;
            return this;
        }

        public Fork create() {
            return new Fork() {
                private final AtomicBoolean block = new AtomicBoolean(false);

                @Override
                public String toString() {
                    return "fork from " + chair.toString();
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
