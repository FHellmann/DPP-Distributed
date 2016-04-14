package edu.hm.cs.vss.rmi;

import edu.hm.cs.vss.Fork;

import java.io.Serializable;

/**
 * Created by Fabio Hellmann on 13.04.2016.
 */
public interface RmiFork extends Serializable {
    static RmiFork convert(final Fork fork) {
        return new RmiFork() {
            @Override
            public String toString() {
                return fork.toString();
            }
        };
    }
}
