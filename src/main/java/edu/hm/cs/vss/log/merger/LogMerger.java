package edu.hm.cs.vss.log.merger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Stream;

/**
 * Created by Fabio on 22.03.2016.
 */
public final class LogMerger {
    private LogMerger() {
    }

    public static void merge(final File directory) {
        final File[] values = directory.listFiles();
        if (values != null && values.length > 0) {
            final SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");

            try {
                final BufferedWriter writer = new BufferedWriter(new FileWriter(new File(directory, "logger-merged.txt")));

                Stream.of(values)
                        .filter(File::isFile)
                        .filter(file -> file.getName().startsWith("log-"))
                        .flatMap(file -> {
                            try {
                                return Files.readAllLines(file.toPath()).stream();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .filter(line -> line != null && line.length() > 0)
                        .sorted((o1, o2) -> {
                            try {
                                final Date timestamp1 = formatter.parse(o1.substring(0, o1.indexOf(" ")));
                                final Date timestamp2 = formatter.parse(o2.substring(0, o2.indexOf(" ")));
                                return timestamp1.compareTo(timestamp2);
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .forEach(line -> {
                            try {
                                writer.write(line + "\n");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });

                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Stream.of(values)
                    .filter(File::isFile)
                    .filter(file -> file.getName().startsWith("log-"))
                    .forEach(File::delete);
        }
    }

    public static void main(String[] args) throws IOException {
        final File file;
        if (args.length == 0) {
            file = new File(".");
            System.out.println("Default Directory: " + file.getAbsolutePath());
        } else {
            file = new File(args[0]);
        }

        merge(file);
    }
}
