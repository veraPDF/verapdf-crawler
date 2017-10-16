package org.verapdf.crawler.health;

import com.codahale.metrics.health.HealthCheck;
import org.verapdf.crawler.core.jobs.HeritrixCleanerService;
import org.verapdf.crawler.tools.AbstractService;

/**
 * @author Maksim Bezrukov
 */
public class ServiceHealthCheck extends HealthCheck {

	private final AbstractService service;

	public ServiceHealthCheck(AbstractService service) {
		this.service = service;
	}

	@Override
	protected Result check() throws Exception {
		if(service.isRunning()) {
			return Result.healthy();
		}
		String message = service.getServiceName() + " stopped";
		String stopReason = service.getStopReason();
		if (stopReason != null) {
			message += ". Reason: " + stopReason;
		}
		return Result.unhealthy(message);
	}
}
