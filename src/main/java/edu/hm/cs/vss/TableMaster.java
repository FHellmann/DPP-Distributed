package edu.hm.cs.vss;

import java.io.Serializable;

/**
 * The Table Master sits at every table. He decides whether a philosopher is allowed to take a seat or not.
 */
public interface TableMaster extends Serializable {
    /**
     * Notify the table master that a new philosopher come to his table.
     *
     * @param philosopher to register.
     */
    default void register(final Philosopher philosopher) {
        // Default: do nothing
    }

    /**
     * Notify the table master that a philosopher disappeared from his table.
     *
     * @param philosopher to unregister.
     */
    default void unregister(final Philosopher philosopher) {
        // Default: do nothing
    }

    /**
     * Check whether the philosopher is allowed to take a seat or not.
     *
     * @param mealCount to check.
     * @return <code>true</code> if the philosopher is allowed to take a seat.
     */
    boolean isAllowedToTakeSeat(final Integer mealCount);
}
