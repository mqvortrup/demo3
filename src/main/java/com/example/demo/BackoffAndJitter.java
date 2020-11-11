package com.example.demo;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Collections.nCopies;

@SpringBootApplication
public class BackoffAndJitter {

    private static final Logger log = LoggerFactory.getLogger(BackoffAndJitter.class);

    private static final int CONCURRENCY = 5;
    private static final ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY);
    private static final Random rand = new Random();
    private static final int WAIT_TIME = 5000;
    private static final int JITTER = 1;
    private static final int BACK_OFF = 1000;
    public static final int MAX_BACK_OFF = 30000;

    public static void main(String[] args) {
        SpringApplication.run(BackoffAndJitter.class);
    }

    //@Bean
    public CommandLineRunner demoManual(CustomerRepository repository) {
        return (args) -> {
            List<Runnable> tasks = nCopies(CONCURRENCY,
                    () -> {
                        // initial back off time for first failure
                        int backOff = BACK_OFF;

                        while (true) {
                            try {
                                // simulate random user actions
                                Thread.sleep(jitter(WAIT_TIME));

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
                                try { Thread.sleep(backOff); } catch (InterruptedException interruptedException) {/*ignore*/}

                                // update back off time for next failed try, but only if limit hasn't been reached
                                if (backOff < MAX_BACK_OFF) {
                                    // simple exponential backoff, other variants exists, e.g. random
                                    backOff *= 2;
                                    // apply jitter so not all users retry at same time
                                    backOff = jitter(backOff);
                                }
                            }
                        }});
            for (Runnable r : tasks) executor.execute(r);
        };
    };

    @Bean
    public CommandLineRunner demoUtility(CustomerRepository repository) {
        return (args) -> {
            // start threads
            for (int i = 0; i < CONCURRENCY; i++) {
                executor.submit(
                        () -> {
                            // configure retry function
                            IntervalFunction intervalFn =
                                    IntervalFunction.ofExponentialRandomBackoff(BACK_OFF, 2, 0.5, MAX_BACK_OFF);

                            RetryConfig retryConfig = RetryConfig.custom()
                                    .intervalFunction(intervalFn)
                                    .build();
                            Retry retry = Retry.of("query db", retryConfig);

                            Runnable query = Retry
                                    .decorateRunnable(retry, () -> {
                                        // query database
                                        int count =  0;
                                        for (Customer customer : repository.findAll()) {
                                            count++;
                                        }
                                        log.info("Count: " + count);
                                    });

                            while (true) {
                                try {
                                    // simulate random user actions
                                    Thread.sleep(jitter(WAIT_TIME));

                                    //execute query
                                    query.run();
                                } catch (Exception e) {
                                    log.error("Exception: " + e.getMessage());
                                }
                            }
                        });
            }
        };
    }

    private int jitter(int waitTime) {
        int jitter = rand.nextInt(waitTime/JITTER) - waitTime/(JITTER*2);
        //int jitter = 0
        return waitTime + jitter;
    }
}