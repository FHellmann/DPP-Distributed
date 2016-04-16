package edu.hm.cs.vss;

import edu.hm.cs.vss.local.LocalTableMaster;
import edu.hm.cs.vss.log.FileLogger;
import edu.hm.cs.vss.log.Logger;
import edu.hm.cs.vss.log.merger.LogMerger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
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
                .withTableMaster(new LocalTableMaster())
                .create();

        final List<Philosopher> philosopherList = IntStream.rangeClosed(1, philosopherCount)
                .mapToObj(index -> new Philosopher.Builder()
                        .setFileLogger()
                        .setTable(table)
                        .setVeryHungry(veryHungry && index == 1)
                        .create())
                .peek(Thread::start)
                .collect(Collectors.toList());

        // ##########################################################################################
        // User Input
        // ##########################################################################################

        final Scanner scanner = new Scanner(System.in);
        String input;
        boolean quit = false;
        while (!quit) {
            System.out.println("Waiting for user input (" + philosopherList.size() + " = Philosophers | " + table.getChairs().count() + " = Chairs)");
            System.out.print("> ");
            input = scanner.nextLine();
            switch (input) {
                case "p":
                case "P":
                    System.out.print("Add philosophers ('-' = delete): ");
                    int count = scanner.nextInt();

                    if (count > 0) {
                        System.out.println("Adding " + count + " Philosopher(s)...");

                        IntStream.rangeClosed(0, count - 1)
                                .mapToObj(index -> new Philosopher.Builder()
                                        .setFileLogger()
                                        .setTable(table)
                                        .create())
                                .peek(philosopherList::add)
                                .forEach(Thread::start);

                        System.out.println("Added " + count + " Philosopher(s)!");
                    } else {
                        count *= -1;

                        System.out.println("Killing " + count + " Philosopher(s)...");
                        if (count < philosopherList.size()) {
                            IntStream.rangeClosed(0, count - 1)
                                    .mapToObj(philosopherList::get)
                                    .peek(Thread::interrupt)
                                    .peek(philosopherList::remove)
                                    .forEach(philosopher -> System.out.println("Killed Philosopher " + philosopher.getName() + "!"));
                        } else {
                            philosopherList.parallelStream()
                                    .forEach(Thread::interrupt);
                            philosopherList.clear();
                            System.out.println("Killed all Philosophers!");
                        }
                    }
                    break;
                case "p -":
                case "P -":
                case "p remove":
                case "P remove":
                    System.out.println("Philosopher(s):");
                    philosopherList.stream()
                            .map(Thread::getName)
                            .forEach(System.out::println);

                    System.out.print("Enter name to kill ('' = all): ");
                    String name = scanner.next();

                    if (name.length() > 0) {
                        System.out.println("Trying to kill " + name + "...");

                        final Optional<Philosopher> any = philosopherList.stream()
                                .filter(philosopher -> philosopher.getName().equals(name))
                                .findAny();
                        if (any.isPresent()) {
                            final Philosopher philosopher = any.get();
                            philosopher.interrupt();
                            philosopherList.remove(philosopher);
                            System.out.println("Killed Philosopher " + philosopher.getName() + "!");
                        } else {
                            System.out.println("Could not found Philosopher called " + name);
                        }
                    } else {
                        philosopherList.parallelStream()
                                .forEach(Thread::interrupt);
                        philosopherList.clear();
                        System.out.println("Killed all Philosophers!");
                    }
                    break;
                case "q":
                case "Q":
                    quit = true;
                    break;
            }
        }

        // ##########################################################################################

        System.out.println("Exit program!");

        // Waiting for all threads to finish
        philosopherList.parallelStream()
                .forEach(Thread::interrupt);
        try {
            Thread.currentThread().join(TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS));
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
