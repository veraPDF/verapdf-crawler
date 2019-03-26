//package org.verapdf.crawler.logius.health;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.actuate.health.Health;
//import org.springframework.boot.actuate.health.HealthIndicator;
//import org.springframework.stereotype.Component;
//import org.verapdf.crawler.logius.core.heritrix.HeritrixClient;
//
//import java.io.IOException;
//
//@Component
//public class HeritrixHealthCheck implements HealthIndicator {
//    private final HeritrixClient client;
//
//    @Autowired
//    public HeritrixHealthCheck(HeritrixClient client) {
//        this.client = client;
//    }
//
//
//    //todo healths
//    @Override
//    public Health health() {
//        try {
//            if (client.testHeritrixAvailability()) {
//                return Health.up().build();
//            }
//            return Health.down().build();
//        } catch (IOException e) {
//            return Health.down().withDetail("Error message", e.getMessage()).build();
//        }
//    }
//}