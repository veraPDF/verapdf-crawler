package org.verapdf.crawler.health;

import com.codahale.metrics.health.HealthCheck;
import org.verapdf.crawler.core.jobs.MonitorCrawlJobStatusService;

public class MonitorCrawlJobStatusServiceHealthCheck extends HealthCheck {

	private final MonitorCrawlJobStatusService service;

	public MonitorCrawlJobStatusServiceHealthCheck(MonitorCrawlJobStatusService service) {
		this.service = service;
	}

	@Override
	protected Result check() throws Exception {
		if (service.isRunning()) {
			return Result.healthy();
		}
		return Result.unhealthy("Monitor crawl job status service stopped");
	}
}
