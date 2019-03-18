package org.verapdf.crawler.logius.resources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.verapdf.crawler.logius.core.tasks.AbstractTask;

import java.util.Map;


/**
 * @author Maksim Bezrukov
 */
@RestController
@RequestMapping(value = "api/admin")
public class AdminResource {
    private final Map<String, AbstractTask> availableServices;

    @Autowired
    public AdminResource(Map<String, AbstractTask> availableServices) {
        this.availableServices = availableServices;
    }

    @PostMapping("/service")
    public ResponseEntity startService(@RequestParam("name") String name) {
        AbstractTask service = availableServices.get(name);
        if (service == null) {
            return ResponseEntity.notFound().build();
        }
        service.start();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ping")
    public ResponseEntity ping() {
        return ResponseEntity.ok().body("ping");
    }
}
