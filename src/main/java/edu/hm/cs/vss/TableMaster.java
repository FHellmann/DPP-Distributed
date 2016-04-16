package edu.hm.cs.vss;

import java.io.Serializable;

/**
 * Created by Fabio Hellmann on 30.03.2016.
 */
public interface TableMaster extends Serializable {
    int MAX_DEVIATION = 10;

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
