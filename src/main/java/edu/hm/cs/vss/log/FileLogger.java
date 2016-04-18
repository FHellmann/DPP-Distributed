package edu.hm.cs.vss.log;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

/**
 * Created by Fabio on 22.03.2016.
 */
public class FileLogger implements Logger {
    private final String fileName;

    public FileLogger(final String name) {
        this.fileName = "log-" + name + ".txt";
    }

    @Override
    public void log(String text) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(fileName, true), true);
            writer.write(String.format(TIMESTAMP_FORMAT, new Date()) + text);
            writer.write("\n");
            writer.flush();
        } catch (IOException e) {
            // Ignore an error
            // e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
