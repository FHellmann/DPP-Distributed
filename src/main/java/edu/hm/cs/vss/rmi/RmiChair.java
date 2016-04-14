package edu.hm.cs.vss.rmi;

import edu.hm.cs.vss.Chair;

import java.io.Serializable;

/**
 * Created by Fabio Hellmann on 13.04.2016.
 */
public interface RmiChair extends Serializable {

    RmiFork getFork();

    static RmiChair convert(final Chair chair) {
        return new RmiChair() {
            @Override
            public RmiFork getFork() {
                return RmiFork.convert(chair.getFork());
            }

            @Override
            public String toString() {
                return chair.toString();
            }
        };
    }
}
