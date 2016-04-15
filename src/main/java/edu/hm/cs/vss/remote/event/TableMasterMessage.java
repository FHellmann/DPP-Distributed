package edu.hm.cs.vss.remote.event;

/**
 * Created by Fabio Hellmann on 14.04.2016.
 */
public class TableMasterMessage extends Message<Integer> {
    public TableMasterMessage(Type type) {
        super(type);
    }

    public TableMasterMessage(Type type, Integer param) {
        super(type, param);
    }
}
