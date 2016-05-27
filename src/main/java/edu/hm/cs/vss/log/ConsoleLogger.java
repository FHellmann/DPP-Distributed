package edu.hm.cs.vss.log;

import java.util.Date;

/**
 * Created by Fabio on 22.03.2016.
 */
public class ConsoleLogger implements Logger {
    @Override
    public void log(String text) {
        System.out.println(String.format(TIMESTAMP_FORMAT, new Date()) + text);
    }
}
