package edu.hm.cs.vss;

import edu.hm.cs.vss.local.LocalTableMaster;
import edu.hm.cs.vss.log.FileLogger;
import edu.hm.cs.vss.log.Logger;
import edu.hm.cs.vss.log.merger.LogMerger;

import java.io.File;
import java.io.IOException;
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
                .setFileLogger()
                .createNetwork();

        // ##########################################################################################
        // User Input
        // ##########################################################################################

        final Scanner scanner = new Scanner(System.in);
        String input;
        boolean quit = false;
        while (!quit) {
            System.out.print("> ");
            input = scanner.nextLine();
            switch (input) {
                /*
                case "?":
                case "h":
                case "H":
                case "help":
                case "Help": {
                    System.out.println("? / h / H / help / Help \t\t Zeigt die Hilfe an.");
                    System.out.println("s / S / status / Backup \t\t Zeigt den Backup an.");
                } break;
                */
                case "s":
                case "S":
                case "status":
                case "Backup": {
                    System.out.println("######################## STATUS ########################");
                    System.out.println("Table(s):");
                    table.getTables().peek(tmp -> System.out.println("\t- " + tmp.getName())).forEach(tmp -> tmp.getChairs().map(Object::toString).map(name -> "\t\t+ " + name).forEach(System.out::println));
                    System.out.println("Local Philosopher(s):");
                    table.getPhilosophers().map(Thread::getName).map(name -> "\t- " + name).forEach(System.out::println);
                    System.out.println("######################### ENDE #########################");
                }
                break;
                case "t":
                case "T":
                case "table":
                case "Table": {
                    System.out.print("Enter host name: ");
                    final String hostName = scanner.next();

                    System.out.println("Try to connect to table " + hostName);
                    table.connectToTable(hostName);
                }
                break;
                case "c":
                case "C":
                case "chair":
                case "Chair": {
                    System.out.print("Enter the chair count to add/delete: ");
                    int count = scanner.nextInt();

                    if (count > 0) {
                        System.out.println("Adding " + count + " Chair(s)...");

                        IntStream.rangeClosed(0, count - 1)
                                .mapToObj(index -> new Chair.Builder().setNameUniqueId().create())
                                .forEach(table::addChair);

                        System.out.println("Added " + count + " Chair(s)!");
                    } else {
                        count *= -1;
                        if (count > table.getChairs().count()) {
                            count = (int) (table.getChairs().count() - 1);
                        }

                        System.out.println("Removing " + count + " Chair(s)...");

                        table.getChairs()
                                .limit(count - 1)
                                .collect(Collectors.toList())
                                .stream().forEach(table::removeChair);

                        System.out.println("Removed " + count + " Chair(s)!");
                    }
                }
                break;
                case "p":
                case "P": {
                    System.out.print("Add philosophers: ");
                    int count = scanner.nextInt();

                    if (count > 0) {
                        System.out.println("Adding " + count + " Philosopher(s)...");

                        IntStream.rangeClosed(0, count - 1)
                                .mapToObj(index -> new Philosopher.Builder()
                                        .setUniqueName()
                                        .setFileLogger()
                                        .setTable(table)
                                        .create())
                                .forEach(table::addPhilosopher);

                        System.out.println("Added " + count + " Philosopher(s)!");
                    }
                }
                break;
                case "p -":
                case "P -":
                case "p remove":
                case "P remove":
                    System.out.println("Philosopher(s):");
                    table.getPhilosophers()
                            .map(Thread::getName)
                            .forEach(System.out::println);

                    System.out.print("Enter name to kill ('' = all): ");
                    String name = scanner.next();

                    if (name.length() > 0) {
                        System.out.println("Trying to kill " + name + "...");

                        final Optional<Philosopher> any = table.getPhilosophers()
                                .filter(philosopher -> philosopher.getName().equals(name))
                                .findAny();
                        if (any.isPresent()) {
                            final Philosopher philosopher = any.get();
                            table.removePhilosopher(philosopher);
                            System.out.println("Killed Philosopher " + philosopher.getName() + "!");
                        } else {
                            System.out.println("Could not found Philosopher called " + name);
                        }
                    } else {
                        table.getPhilosophers().collect(Collectors.toList())
                                .stream()
                                .forEach(table::removePhilosopher);
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
        table.getPhilosophers().parallel()
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
        table.getPhilosophers()
                .map(philosopher -> philosopher.getName() + ": " + philosopher.getMealCount())
                .forEach(logger::log);
        logger.log("############### END #################");

        // Merge all log files
        final File file = new File(".");
        LogMerger.merge(file);

        System.out.println("Files merged!");

        System.exit(1);
    }
}
