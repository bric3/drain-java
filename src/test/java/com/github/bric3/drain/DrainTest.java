package com.github.bric3.drain;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @title: DrainTest
 * @description: TODO
 * @date 2021-07-22 16:18
 */
public class DrainTest {

    @Test
    public void test() throws IOException {
        Drain drain = Drain.drainBuilder()
                .additionalDelimiters("_")
                .depth(4)
                .build();
        Files.lines(Paths.get("doc/message.log"), StandardCharsets.UTF_8).forEach(drain::parseLogMessage);
        List<LogCluster> clusters = drain.clusters();

//        AtomicInteger lineCounter = new AtomicInteger();
//        Files.lines(Paths.get("build/resources/test/SSH.log"),
//                StandardCharsets.UTF_8)
//                .peek(__ -> lineCounter.incrementAndGet())
//                .map(l -> l.substring(l.indexOf("]: ") + 3)) // removes this part: "Dec 10 06:55:46 LabSZ sshd[24200]: "
//                .forEach(content -> {
//                    drain.parseLogMessage(content);
//                    if (lineCounter.get() % 10000 == 0) {
//                        System.out.printf("%4d clusters so far%n", drain.clusters().size());
//                    }
//                });


        drain.clusters()
                .stream()
                .sorted(Comparator.comparing(LogCluster::sightings).reversed())
                .forEach(System.out::println);
    }

    @Test
    public void testAIOpsWebLogic() throws IOException {
        Drain drain = Drain.drainBuilder()
                .additionalDelimiters("_")
                .depth(4)
                .build();

        AtomicInteger lineCounter = new AtomicInteger();
        Files.lines(Paths.get("doc/log_weblogic_0128_sample_cut.csv"),
                StandardCharsets.UTF_8)
//                .peek(__ -> lineCounter.incrementAndGet())
//                .map(l -> l.substring(l.indexOf("]: ") + 3)) // removes this part: "Dec 10 06:55:46 LabSZ sshd[24200]: "
                .forEach(content -> {
                    drain.parseLogMessage(content);
//                    if (lineCounter.get() % 10000 == 0) {
//                        System.out.printf("%4d clusters so far%n", drain.clusters().size());
//                    }
                });

        drain.clusters()
                .stream()
                .sorted(Comparator.comparing(LogCluster::sightings).reversed())
                .forEach(System.out::println);
    }

    @Test
    public void testAIOpsZKlog() throws IOException {
        Drain drain = Drain.drainBuilder()
                .additionalDelimiters("_")
                .depth(4)
                .build();

        AtomicInteger lineCounter = new AtomicInteger();
        Files.lines(Paths.get("doc/log_zk_0128_cut.csv"),
                StandardCharsets.UTF_8)
//                .peek(__ -> lineCounter.incrementAndGet())
//                .map(l -> l.substring(l.indexOf("]: ") + 3)) // removes this part: "Dec 10 06:55:46 LabSZ sshd[24200]: "
                .forEach(content -> {
                    drain.parseLogMessage(content);
//                    if (lineCounter.get() % 10000 == 0) {
//                        System.out.printf("%4d clusters so far%n", drain.clusters().size());
//                    }
                });

        drain.clusters()
                .stream()
                .sorted(Comparator.comparing(LogCluster::sightings).reversed())
                .forEach(System.out::println);
    }
}
