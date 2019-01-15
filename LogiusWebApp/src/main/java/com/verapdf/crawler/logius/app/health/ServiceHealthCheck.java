package com.verapdf.crawler.logius.app.health;

import com.verapdf.crawler.logius.app.tools.AbstractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;


@Component
public class ServiceHealthCheck implements HealthIndicator {

    private final Map<String, AbstractService> availableServices;

    @Autowired
    public ServiceHealthCheck(Map<String, AbstractService> availableServices) {
        this.availableServices = availableServices;
    }


    @Override
    public Health health() {
        Health.Builder health = new Health.Builder();

        for (Map.Entry<String, AbstractService> entry : availableServices.entrySet()) {
            if (entry.getValue().isRunning()) {
                health.up().withDetail(entry.getKey(), "is running");
            } else {
                String message = " stopped.";
                if (entry.getValue().getStopReason() != null) {
                    message += " Reason: " + entry.getValue().getStopReason();
                }
                health.down().withDetail(entry.getKey(), message);
            }
        }

        return health.build();
    }
}
