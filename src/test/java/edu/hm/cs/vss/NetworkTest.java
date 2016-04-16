package edu.hm.cs.vss;

import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.stream.IntStream;

/**
 * Created by Fabio Hellmann on 13.04.2016.
 */
public class NetworkTest {
    @Test
    public void testFindClients() throws SocketException, UnknownHostException {
        /*
        IntStream.rangeClosed(1, 255).parallel()
                .mapToObj(Integer::toString)
                .flatMap(first -> IntStream.rangeClosed(1, 255).parallel()
                        .mapToObj(Integer::toString)
                        .flatMap(second -> IntStream.rangeClosed(1, 255).parallel()
                                .mapToObj(Integer::toString)
                                .flatMap(third -> IntStream.rangeClosed(1, 255).parallel()
                                        .mapToObj(Integer::toString)
                                        .map(fourth -> String.format("%03d.%03d.%03d.%03d", Integer.parseInt(first), Integer.parseInt(second), Integer.parseInt(third), Integer.parseInt(fourth))))))
                .filter(host -> {
                    try {
                        return InetAddress.getByName(host).isReachable(50);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .forEach(System.out::println);
                */
    }
}
