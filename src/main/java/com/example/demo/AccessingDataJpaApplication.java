package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class AccessingDataJpaApplication {

    private static final Logger log = LoggerFactory.getLogger(AccessingDataJpaApplication.class);

    private static final int CONCURRENCY = 5;
    private static final ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY);
    private static final Random rand = new Random();
    private static final int WAIT_TIME = 5000;
    private static final int JITTER = 1;
    private static final int BACK_OFF = 1000;
    public static final int MAX_BACK_OFF = 30000;

    public static void main(String[] args) {
        SpringApplication.run(AccessingDataJpaApplication.class);
    }

    @Bean
    public CommandLineRunner demo(CustomerRepository repository) {
        return (args) -> {
            // start threads
            for (int i = 0; i < CONCURRENCY; i++) {
                executor.submit(
                        () -> {
                            // initial back off time
                            int backOff = BACK_OFF;

                            while (true) {
                                try {
                                    // simulate random user actions
                                    Thread.sleep(WAIT_TIME + jitter(WAIT_TIME));

                                    // query database
                                    int count =  0;
                                    for (Customer customer : repository.findAll()) {
                                        count++;
                                    }
                                    log.info("Count: " + count);

                                    // call succeeded, so reset back off
                                    backOff = BACK_OFF;
                                } catch (Exception e) {
                                    log.error("Exception: " + e.getMessage());

                                    // back off for currently set time
                                    log.warn("Backing off: " + backOff);
                                    Thread.sleep(backOff);

                                    // update back off time for next failed try, but only if limit hasn't been reached
                                    if (backOff < MAX_BACK_OFF) {
                                        backOff *= 2;
                                        // apply jitter so not all users retry at same time
                                        backOff += jitter(backOff);
                                    }
                                }
                        }});
            }
        };
    }

    private int jitter(int waitTime) {
        int jitter = rand.nextInt(waitTime/JITTER) - waitTime/(JITTER*2);
        //int jitter = 0
        return waitTime + jitter;
    }

}