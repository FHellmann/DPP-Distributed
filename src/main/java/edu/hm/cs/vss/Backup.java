package edu.hm.cs.vss;

import java.io.Serializable;

/**
 * Created by Fabio on 17.04.2016.
 */
public interface Backup extends Serializable {
    int getCores();

    int getChairCount();

    int getPhilosopherCount();

    String getPhilosopherName(final int index);

    int getPhilosopherMealCount(final int index);

    boolean isPhilosopherHungry(final int index);
}
