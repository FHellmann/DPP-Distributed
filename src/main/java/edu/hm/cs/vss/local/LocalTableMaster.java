package edu.hm.cs.vss.local;

import edu.hm.cs.vss.Philosopher;
import edu.hm.cs.vss.TableMaster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This Table Master makes the decision due to the amount of meals the philosopher has already eaten.
 */
public class LocalTableMaster implements TableMaster, Philosopher.OnStandUpListener {
    private final List<Philosopher> philosopherList = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger maxMealCount = new AtomicInteger(MAX_DEVIATION);

    @Override
    public void register(Philosopher philosopher) {
        philosopherList.add(philosopher);
        philosopher.addOnStandUpListener(this);
    }

    @Override
    public void unregister(Philosopher philosopher) {
        philosopherList.remove(philosopher);
        philosopher.removeOnStandUpListener(this);
    }

    @Override
    public boolean isAllowedToTakeSeat(Integer mealCount) {
        return mealCount <= maxMealCount.get();
    }

    @Override
    public void onStandUp(Philosopher philosopher) {
        // Calculate the minimal amount of meals the philosophers from this table master have eaten.
        maxMealCount.set(philosopherList.parallelStream()
                .mapToInt(Philosopher::getMealCount)
                .min()
                .orElse(0) + MAX_DEVIATION);
    }
}
