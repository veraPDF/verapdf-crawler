package com.verapdf.crawler.logius.app.health;

import com.codahale.metrics.health.HealthCheck;

import com.verapdf.crawler.logius.app.core.heritrix.HeritrixClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class HeritrixHealthCheck implements HealthIndicator {
    private final HeritrixClient client;

    @Autowired
    public HeritrixHealthCheck(HeritrixClient client) {
        this.client = client;
    }


    @Override
    public Health health() {
        try {
            if (client.testHeritrixAvailability()) {
                return Health.up().build();
            }
            return Health.down().build();
        } catch (IOException e) {
            return Health.down().withDetail("Error message", e.getMessage()).build();
        }
    }
}