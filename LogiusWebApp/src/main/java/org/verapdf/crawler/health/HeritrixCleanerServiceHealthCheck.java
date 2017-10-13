package org.verapdf.crawler.health;

import com.codahale.metrics.health.HealthCheck;
import org.verapdf.crawler.core.jobs.HeritrixCleanerService;

/**
 * @author Maksim Bezrukov
 */
public class HeritrixCleanerServiceHealthCheck  extends HealthCheck {

	private final HeritrixCleanerService service;

	public HeritrixCleanerServiceHealthCheck(HeritrixCleanerService service) {
		this.service = service;
	}

	@Override
	protected Result check() throws Exception {
		if (service.isRunning()) {
			return Result.healthy();
		}
		return Result.unhealthy("Heritrix cleaner service stopped");
	}
}
