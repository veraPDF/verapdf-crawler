package com.verapdf.crawler.logius.app.resources;

import com.verapdf.crawler.logius.app.tools.AbstractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


/**
 * @author Maksim Bezrukov
 */
@RestController
@RequestMapping(value = "logius/admin", produces = MediaType.APPLICATION_JSON_VALUE)
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
			//todo mb return 404?
			return ResponseEntity.notFound().build();
			//throw new WebApplicationException("Service " + name + " not found", Response.Status.NOT_FOUND);
		}
		service.start();
		return ResponseEntity.ok().build();
	}
}
