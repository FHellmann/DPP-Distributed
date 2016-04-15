package edu.hm.cs.vss.remote.event;

import java.util.Optional;

/**
 * Created by Fabio Hellmann on 14.04.2016.
 */
public abstract class Message<T> {
    private final Type type;
    private final T param;

    public Message(final Type type) {
        this(type, null);
    }

    public Message(final Type type, final T param) {
        this.type = type;
        this.param = param;
    }

    public Type getType() {
        return type;
    }

    public Optional<T> getParam() {
        return Optional.ofNullable(param);
    }

    public enum Type {
        DELETE_TABLE, GET_CHAIRS, GET_MIN_MEAL_COUNT, ADD_TABLE
    }
}
