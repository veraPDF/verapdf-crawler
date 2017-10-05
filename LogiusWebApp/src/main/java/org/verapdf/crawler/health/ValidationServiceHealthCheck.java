package org.verapdf.crawler.health;

import com.codahale.metrics.health.HealthCheck;
import org.verapdf.crawler.core.validation.ValidationService;

public class ValidationServiceHealthCheck extends HealthCheck {

	private final ValidationService validationService;

	public ValidationServiceHealthCheck(ValidationService validationService) {
		this.validationService = validationService;
	}

	@Override
	protected Result check() throws Exception {
		if(validationService.isRunning()) {
			return Result.healthy();
		}
		String message = "Validation service stopped";
		String stopReason = validationService.getStopReason();
		if (stopReason != null) {
			message += ". Reason: " + stopReason;
		}
		return Result.unhealthy(message);
	}
}
