package com.verapdf.crawler.logius.app.resources;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author Maksim Bezrukov
 */
@RestController
@RequestMapping(value = "/api/admin", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminResource {



	@PostMapping("/service")
	public ResponseEntity startService(@RequestParam("name") String name) {
//		AbstractService service = resourceManager.getService(name);
//		if (service == null) {
//			//todo mb return 404?
//			return ResponseEntity.notFound().build();
//			//throw new WebApplicationException("Service " + name + " not found", Response.Status.NOT_FOUND);
//		}
//		service.start();

		return ResponseEntity.ok().build();
	}
}
