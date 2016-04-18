package edu.hm.cs.vss;

import java.io.Serializable;

/**
 * The Backup safes all important information from a system.
 */
public interface Backup extends Serializable {
    /**
     * Get the host of this backup.
     *
     * @return the host.
     */
    String getHost();

    /**
     * Get the amount of cpu cores from the computer/server of this backup.
     *
     * @return the amount of cpu cores.
     */
    int getCores();

    /**
     * Get the chair count.
     *
     * @return the chair count.
     */
    int getChairCount();

    /**
     * Get the philosophers count.
     *
     * @return the philosophers count.
     */
    int getPhilosopherCount();

    /**
     * Get the philosophers name.
     *
     * @param index of the philosopher.
     * @return the name.
     */
    String getPhilosopherName(final int index);

    /**
     * Get the philosophers meal count.
     *
     * @param index of the philosopher.
     * @return the meal count.
     */
    int getPhilosopherMealCount(final int index);

    /**
     * Is the philosophers hungry.
     *
     * @param index of the philosopher.
     * @return the hungry status.
     */
    boolean isPhilosopherHungry(final int index);
}
