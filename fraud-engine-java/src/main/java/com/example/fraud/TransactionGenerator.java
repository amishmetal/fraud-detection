package com.example.fraud;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/** Simulates a live stream of bank transactions, with some suspicious ones mixed in. */
@Component
public class TransactionGenerator {

    private static final List<String> COUNTRIES = List.of("IN", "US", "GB", "AE", "SG");
    private final TransactionService service;
    private final Random random = new Random();

    public TransactionGenerator(TransactionService service) {
        this.service = service;
    }

    @Scheduled(fixedRate = 2000, initialDelay = 5000)
    public void generate() {
        long account = 1 + random.nextInt(10);
        String home = COUNTRIES.get((int) (account % COUNTRIES.size()));

        // Mostly normal behaviour
        String country = random.nextInt(100) < 10 ? COUNTRIES.get(random.nextInt(COUNTRIES.size())) : home;
        double amount = random.nextInt(100) < 5 ? 5001 + random.nextInt(20000) : 20 + random.nextInt(900);

        service.process(new TransactionService.Tx(account, amount, country));

        // Occasional burst to trigger the velocity rule
        if (random.nextInt(100) < 3) {
            for (int i = 0; i < 6; i++) {
                service.process(new TransactionService.Tx(account, 50 + random.nextInt(100), home));
            }
        }
    }
}
