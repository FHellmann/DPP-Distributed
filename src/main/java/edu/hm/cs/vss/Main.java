package edu.hm.cs.vss;

import edu.hm.cs.vss.impl.TableMasterMealObserver;
import edu.hm.cs.vss.log.FileLogger;
import edu.hm.cs.vss.log.Logger;
import edu.hm.cs.vss.log.merger.LogMerger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Fabio Hellmann on 16.03.2016.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        System.out.println("java -jar Program <runtime> <philosophers> <chairs> <ishungry>");

        final long runtime; // Duration of the program activity
        final int philosopherCount; // Amount of philosophers
        final int chairCount; // Amount of chairs
        final boolean veryHungry; // Is the first philosopher very hungry?
        if (args.length == 4) {
            // Manual user input
            int index = 0;
            runtime = TimeUnit.MILLISECONDS.convert(Long.parseLong(args[index++]), TimeUnit.SECONDS);
            philosopherCount = Integer.parseInt(args[index++]);
            chairCount = Integer.parseInt(args[index++]);
            veryHungry = Boolean.parseBoolean(args[index]);
        } else {
            // Defaults
            runtime = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
            philosopherCount = 5;
            chairCount = 5;
            veryHungry = false;
        }

        System.out.println("java -jar Program <runtime=" + runtime + "> <philosophers=" + philosopherCount +
                "> <chairs=" + chairCount + "> <ishungry=" + veryHungry + ">");

        final Table table = new Table.Builder()
                .withChairCount(chairCount)
                .withTableMaster(new TableMasterMealObserver())
                .create();

        final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
            final Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });

        final List<Philosopher> philosopherList = IntStream.rangeClosed(1, philosopherCount)
                .mapToObj(index -> new Philosopher.Builder()
                        .setFileLogger()
                        .setTable(table)
                        .setVeryHungry(veryHungry && index == 1)
                        .create())
                .peek(executorService::execute)
                .collect(Collectors.toList());

        try {
            Thread.sleep(runtime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Exit program! [Runtime = " + runtime + "]");

        // Waiting for all threads to finish
        executorService.shutdownNow();
        try {
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Threads terminated");

        Logger logger = new FileLogger("statistic");
        logger.log("############# Statistic #############");
        logger.log("# Hardware");
        logger.log("CPU-Cores (available to the JVM) = " + Runtime.getRuntime().availableProcessors());
        logger.log("Memory (available to the JVM) = " + Runtime.getRuntime().maxMemory());
        logger.log("# Philosophers");
        philosopherList.stream()
                .map(philosopher -> philosopher.getName() + ": " + philosopher.getMealCount())
                .forEach(logger::log);
        logger.log("############### END #################");

        // Merge all log files
        final File file = new File(".");
        LogMerger.merge(file);
    }
}
