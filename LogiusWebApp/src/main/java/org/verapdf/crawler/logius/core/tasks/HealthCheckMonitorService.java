//package org.verapdf.crawler.core.tasks;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import SendEmail;
//import org.verapdf.crawler.resources.HealthResource;
//import AbstractTask;
//
//import java.io.IOException;
//import java.util.*;
//
///**
// * @author Maksim Bezrukov
// */
//static class HealthCheckMonitorService extends AbstractTask {
//
//	private static final long SLEEP_DURATION = 60*1000;
//
//	private static final String EMAIL_SUBJECT = "Logius health checks fails";
//	private static final String EMAIL_BODY = "Checks fail for health check(s): %s";
//
//	private final HealthResource healthResource;
//	private final Set<String> excludeChecks;
//	private final Set<String> notifiedChecks = new HashSet<>();
//
//	static HealthCheckMonitorService(HealthResource healthResource, List<String> excludeChecks) {
//		super("HealthCheckMonitorService", SLEEP_DURATION);
//		this.healthResource = healthResource;
//		this.excludeChecks = excludeChecks == null ? new HashSet<>() : new HashSet<>(excludeChecks);
//	}
//
//	@Override
//	protected void onStart() throws InterruptedException {
//		// we prefer to sleep on start before 1st check to avoid sending emails
//		// during tasks startup
//		Thread.sleep(SLEEP_DURATION);
//	}
//
//	@Override
//	protected boolean onRepeat() throws IOException {
//		String healthCheck = healthResource.getHealthCheck();
//		Set<String> failedChecks = new HashSet<>();
//		ObjectMapper mapper = new ObjectMapper();
//		JsonNode rootNode = mapper.readValue(healthCheck, JsonNode.class);
//		Iterator<Map.Entry<String, JsonNode>> it = rootNode.fields();
//		while(it.hasNext()) {
//			Map.Entry<String, JsonNode> nodeEntry = it.next();
//			String name = nodeEntry.getKey();
//			if (!excludeChecks.contains(name)) {
//				boolean healthy = nodeEntry.getValue().get("healthy").asBoolean();
//				boolean notified = notifiedChecks.contains(name);
//				if (healthy && notified) {
//					notifiedChecks.remove(name);
//				} else if (!healthy && !notified) {
//					failedChecks.add(name);
//				}
//			}
//		}
//		if (!failedChecks.isEmpty()) {
//			StringBuilder builder = new StringBuilder();
//			for (String check : failedChecks) {
//				this.notifiedChecks.add(check);
//				builder.append(check).append(", ");
//			}
//			SendEmail.sendReportNotification(EMAIL_SUBJECT, String.format(EMAIL_BODY, builder.substring(0, builder.length()-2)));
//		}
//		return true;
//	}
//}
