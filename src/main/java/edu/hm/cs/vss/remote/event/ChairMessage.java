package edu.hm.cs.vss.remote.event;

import edu.hm.cs.vss.Chair;

import java.util.List;

/**
 * Created by Fabio Hellmann on 14.04.2016.
 */
public class ChairMessage extends Message<List<Chair>> {
    public ChairMessage(Type type) {
        super(type);
    }

    public ChairMessage(Type type, List<Chair> param) {
        super(type, param);
    }
}
