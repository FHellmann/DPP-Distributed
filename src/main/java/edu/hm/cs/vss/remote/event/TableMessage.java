package edu.hm.cs.vss.remote.event;

/**
 * Created by Fabio Hellmann on 14.04.2016.
 */
public class TableMessage extends Message<String> {
    public TableMessage(Type type) {
        super(type);
    }

    public TableMessage(Type type, String param) {
        super(type, param);
    }
}
