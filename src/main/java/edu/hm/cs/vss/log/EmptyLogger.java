package edu.hm.cs.vss.log;

/**
 * Created by Fabio Hellmann on 23.03.2016.
 */
public class EmptyLogger implements Logger {
    @Override
    public void log(String text) {
        // do nothing
    }
}
