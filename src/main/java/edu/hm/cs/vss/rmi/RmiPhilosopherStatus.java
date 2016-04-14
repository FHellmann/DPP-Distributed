package edu.hm.cs.vss.rmi;

import java.io.Serializable;
import java.util.Optional;

/**
 * Created by Fabio Hellmann on 13.04.2016.
 */
public interface RmiPhilosopherStatus extends Serializable {
    int getMealCount();

    Optional<RmiChair> getChair();

    void block();

    void unblock();

    boolean isBlocked();
}
