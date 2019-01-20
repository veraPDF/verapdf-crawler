package org.verapdf.crawler.logius.resources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.verapdf.crawler.logius.tools.AbstractService;

import java.util.Map;


/**
 * @author Maksim Bezrukov
 */
@RestController
@RequestMapping(value = "api/admin", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminResource {
    private final Map<String, AbstractService> availableServices;

    @Autowired
    public AdminResource(Map<String, AbstractService> availableServices) {
        this.availableServices = availableServices;
    }

    @PostMapping("/service")
    public ResponseEntity startService(@RequestParam("name") String name) {
        AbstractService service = availableServices.get(name);
        if (service == null) {
            return ResponseEntity.notFound().build();
        }
        service.start();
        return ResponseEntity.ok().build();
    }
}
