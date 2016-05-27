package edu.hm.cs.vss.log;

/**
 * Created by Fabio on 22.03.2016.
 */
public interface Logger {
    String TIMESTAMP_FORMAT = "%1$tH:%1$tM:%1$tS.%1$tL ";

    /**
     * Write a log message.
     *
     * @param text to log.
     */
    void log(final String text);
}
