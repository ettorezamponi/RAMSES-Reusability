package ramses.dashboard.adapters;

import ramses.dashboard.externalinterfaces.AnalyseClient;
import ramses.dashboard.domain.DashboardWebService;
import ramses.dashboard.externalinterfaces.MonitorClient;
import ramses.knowledge.domain.Modules;
import ramses.knowledge.domain.adaptation.options.AdaptationOption;
import ramses.knowledge.domain.adaptation.specifications.QoSSpecification;
import ramses.knowledge.domain.adaptation.specifications.Availability;
import ramses.knowledge.domain.adaptation.specifications.AverageResponseTime;
import ramses.knowledge.domain.adaptation.values.QoSHistory;
import ramses.knowledge.domain.architecture.Instance;
import ramses.knowledge.domain.architecture.Service;
import ramses.knowledge.domain.architecture.ServiceConfiguration;
import ramses.knowledge.domain.architecture.ServiceImplementation;
import ramses.knowledge.domain.metrics.CircuitBreakerMetrics;
import ramses.knowledge.domain.metrics.HttpEndpointMetrics;
import ramses.knowledge.domain.metrics.InstanceMetricsSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@Slf4j
public class DashboardWebController {
	@Value("${MAX_HISTORY_SIZE}")
	private int maxHistorySize;
	@Value("${ADAPTATION_HISTORY_SIZE}")
	private int adaptationHistorySize;

	@Autowired 
	private DashboardWebService dashboardWebService;

	final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@PostConstruct
	private void init() {
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	/* Home Page */
	@GetMapping("/")
	public String index(Model model) {
		Collection<Service> services = dashboardWebService.getArchitecture().values();
		// <serviceId, [[Property, Value]]>
		Map<String, List<String[]>> servicesConfigurationTable = new HashMap<>();
		// <serviceId, [[QoSName, Value, Threshold, Weight]]>
		Map<String, List<String[]>> servicesQoSTable = new HashMap<>();
		Map<String, List<String[]>> servicesCurrentImplementationTable = new HashMap<>();
		for (Service s : services) {
			ServiceConfiguration conf = s.getConfiguration();
			// List <CustomPropertyName, Value>
			List<String[]> table = new ArrayList<>();
			table.add(new String[]{"Time Of Snapshot", sdf.format(conf.getTimestamp())+" UTC"});
			table.add(new String[]{"", ""});
			table.add(new String[]{"Load Balancer Type", conf.getLoadBalancerType().name().replace("_", " ")});
			table.add(new String[]{"", ""});
			for (ServiceConfiguration.CircuitBreakerConfiguration cbConf : conf.getCircuitBreakersConfiguration().values()) {
				table.add(new String[]{"Circuit Breaker Name", cbConf.getCircuitBreakerName()});
				table.add(new String[]{"Failure Rate Threshold", cbConf.getFailureRateThreshold()+"%"});
				table.add(new String[]{"Slow Call Rate Threshold", cbConf.getSlowCallRateThreshold()+"%"});
				table.add(new String[]{"Slow Call Duration Threshold", cbConf.getSlowCallDurationThreshold()+"ms"});
				table.add(new String[]{"Minimum Number Of Calls", String.valueOf(cbConf.getMinimumNumberOfCalls())});
				table.add(new String[]{"Number Of Calls In Half-Open", cbConf.getPermittedNumberOfCallsInHalfOpenState().toString()});
				table.add(new String[]{"Wait Duration In Open", cbConf.getWaitDurationInOpenState() +"ms"});
				table.add(new String[]{"Event Consumer Buffer Size", String.valueOf(cbConf.getEventConsumerBufferSize())});
				table.add(new String[]{"Sliding Window Size", String.valueOf(cbConf.getSlidingWindowSize())});
				table.add(new String[]{"Sliding Window Type", String.valueOf(cbConf.getSlidingWindowType()).replace("_", " ")});
				table.add(new String[]{"", ""});
			}
			table.remove(table.size() - 1);
			servicesConfigurationTable.put(s.getServiceId(), table);

			// List <QoSName, Value, Threshold, Weight>
			List<String[]> serviceQoSTable = new ArrayList<>();
			QoSHistory<? extends QoSSpecification> serviceAvailability = s.getCurrentImplementation().getQoSCollection().getQoSHistoryMap().get(Availability.class);
			QoSHistory<? extends QoSSpecification> serviceART = s.getCurrentImplementation().getQoSCollection().getQoSHistoryMap().get(AverageResponseTime.class);
			serviceQoSTable.add(new String[]{
					"Availability",
					serviceAvailability.getCurrentValue() == null ? "N/A" : String.format(Locale.ROOT,"%.2f", serviceAvailability.getCurrentValue().getDoubleValue()*100)+"%",
					serviceAvailability.getSpecification().getConstraintDescription(),
					serviceAvailability.getSpecification().getWeight().toString()}
			);
			serviceQoSTable.add(new String[]{
					"Average Response Time [ms]",
					serviceART.getCurrentValue() == null ? "N/A" : String.format(Locale.ROOT,"%.1f", serviceART.getCurrentValue().getDoubleValue()),
					serviceART.getSpecification().getConstraintDescription(),
					serviceART.getSpecification().getWeight().toString()}
			);
			servicesQoSTable.put(s.getServiceId(), serviceQoSTable);

			ServiceImplementation currentImplementation = s.getCurrentImplementation();
			List<String[]> currentImplementationTable = new ArrayList<>();
			currentImplementationTable.add(new String[]{"Implementation Id", currentImplementation.getImplementationId()});
			currentImplementationTable.add(new String[]{"Preference", String.valueOf(currentImplementation.getPreference())});
			currentImplementationTable.add(new String[]{"Trust", String.valueOf(currentImplementation.getTrust())});
			currentImplementationTable.add(new String[]{"Penalty", String.valueOf(currentImplementation.getPenalty())});
			servicesCurrentImplementationTable.put(s.getServiceId(), currentImplementationTable);
		}

		model.addAttribute("servicesIds", services.stream().map(Service::getServiceId).toList());
		model.addAttribute("servicesConfigurationTable", servicesConfigurationTable);
		model.addAttribute("servicesQoSTable", servicesQoSTable);
		model.addAttribute("servicesCurrentImplementationTable", servicesCurrentImplementationTable);
		return "index";
	}


	@GetMapping("/service/{serviceId}")
	public String serviceDetails(Model model, @PathVariable String serviceId) {
		Service service = dashboardWebService.getService(serviceId);
		// [[InstanceId, Status, LatestMetricsDescription]]
		List<String[]> instancesTable = new ArrayList<>();
		for (Instance instance : service.getInstances()) {
			Double weight = null;
			if (service.getConfiguration().getLoadBalancerType() != ServiceConfiguration.LoadBalancerType.UNKNOWN)
				weight = service.getConfiguration().getLoadBalancerWeights().get(instance.getInstanceId());
			instancesTable.add(new String[]{instance.getInstanceId(), instance.getCurrentStatus().toString(), weight == null ? "N/A" : String.format(Locale.ROOT,"%.3f", weight)});
		}
		// [[QoSName, Value, Timestamp]]
		List<String[]> qoSCurrentValueTable = new ArrayList<>();
		QoSHistory.Value availability = service.getCurrentValueForQoS(Availability.class);
		QoSHistory.Value art = service.getCurrentValueForQoS(AverageResponseTime.class);
		qoSCurrentValueTable.add(new String[]{
				"Availability",
				availability == null ? "N/A" : String.format(Locale.ROOT,"%.2f", availability.getDoubleValue()*100)+"%",
				availability == null ? "N/A" : sdf.format(availability.getTimestamp())+" UTC"
		});
		qoSCurrentValueTable.add(new String[]{
				"Average Response Time [ms]",
				art == null ? "N/A" : String.format(Locale.ROOT, "%.1f", art.getDoubleValue()),
				art == null ? "N/A" : sdf.format(art.getTimestamp()) + " UTC"
		});
		model.addAttribute("serviceId", serviceId);
		model.addAttribute("isLoadBalanced", service.getConfiguration().getLoadBalancerType() != ServiceConfiguration.LoadBalancerType.UNKNOWN);
		model.addAttribute("latestAdaptationDate", service.getLatestAdaptationDate());
		model.addAttribute("qoSCurrentValueTable", qoSCurrentValueTable);
		model.addAttribute("possibleImplementations", service.getPossibleImplementations().keySet());
		model.addAttribute("instancesTable", instancesTable);
		model.addAttribute("graphs", computeServiceGraphs(service));
		return "webpages/serviceDetails";
	}

	@GetMapping("/service/{serviceId}/{instanceId}")
	public String instanceDetails(Model model, @PathVariable String serviceId, @PathVariable String instanceId) {
		Service service = dashboardWebService.getService(serviceId);
		Instance instance = dashboardWebService.getInstance(serviceId, instanceId);
		InstanceMetricsSnapshot latestMetrics = instance.getLatestInstanceMetricsSnapshot();
		List<String[]> resourceTable = new ArrayList<>();
		List<String[]> httpMetricsTable = new ArrayList<>();
		List<String[]> circuitBreakersTable = new ArrayList<>();
		// [[QoSName, Value, Timestamp]]
		List<String[]> qoSCurrentValueTable = new ArrayList<>();
		if (latestMetrics != null) {
			resourceTable.add(new String[]{"CPU Usage", "" + String.format(Locale.ROOT, "%.2f", latestMetrics.getCpuUsage()*100)+"%"});
			resourceTable.add(new String[]{"Disk Free Space", String.format(Locale.ROOT, "%.2f", latestMetrics.getDiskFreeSpace()/1024/1024/1024)+" GB"});
			resourceTable.add(new String[]{"Disk Total Space", String.format(Locale.ROOT, "%.2f", latestMetrics.getDiskTotalSpace()/1024/1024/1024)+" GB"});
			for (HttpEndpointMetrics httpMetrics : latestMetrics.getHttpMetrics().values())
				for(String outcome : httpMetrics.getOutcomes())
					httpMetricsTable.add(new String[]{httpMetrics.getHttpMethod() + " " + httpMetrics.getEndpoint(),
						outcome, String.valueOf(httpMetrics.getCountByOutcome(outcome)),
							httpMetrics.getAverageDurationByOutcome(outcome)==-1 ? "N/A" : String.format(Locale.ROOT,"%.1f", httpMetrics.getAverageDurationByOutcome(outcome))+" ms"});
			for (CircuitBreakerMetrics cbMetrics : latestMetrics.getCircuitBreakerMetrics().values()) {
				circuitBreakersTable.add(new String[]{"Circuit Breaker Name", cbMetrics.getName()});
				double failureRate = cbMetrics.getFailureRate();
				circuitBreakersTable.add(new String[]{"Failure Rate", failureRate==-1 ? "N/A" : String.format(Locale.ROOT, "%.1f", failureRate)+"%"});
				circuitBreakersTable.add(new String[]{"Failed Calls Count", cbMetrics.getNotPermittedCallsCount()+""});
				double slowCallRate = cbMetrics.getSlowCallRate();
				circuitBreakersTable.add(new String[]{"Slow Calls Rate", slowCallRate==-1 ? "N/A" : String.format(Locale.ROOT, "%.1f", slowCallRate)+"%"});
				circuitBreakersTable.add(new String[]{"Slow Calls Count", cbMetrics.getSlowCallCount()+""});
				for (CircuitBreakerMetrics.CallOutcomeStatus status : CircuitBreakerMetrics.CallOutcomeStatus.values()) {
					Double avgDuration = cbMetrics.getAverageDuration(status);
					circuitBreakersTable.add(new String[]{"Average Call Duration when "+status, avgDuration.isNaN() ? "N/A" : String.format(Locale.ROOT, "%.3f", avgDuration)+" s"});
				}
				circuitBreakersTable.add(new String[]{"", ""});
			}
			QoSHistory.Value availability = instance.getCurrentValueForQoS(Availability.class);
			QoSHistory.Value art = instance.getCurrentValueForQoS(AverageResponseTime.class);
			qoSCurrentValueTable.add(new String[]{
					"Availability",
					availability == null ? "N/A" : String.format(Locale.ROOT,"%.2f", availability.getDoubleValue()*100)+"%",
					availability == null ? "N/A" : sdf.format(availability.getTimestamp())+" UTC"
			});
			qoSCurrentValueTable.add(new String[]{
					"Average Response Time [ms]",
					art == null ? "N/A" : String.format(Locale.ROOT, "%.1f", art.getDoubleValue()),
					art == null ? "N/A" : sdf.format(art.getTimestamp()) + " UTC"
			});
		}
		model.addAttribute("qoSCurrentValueTable", qoSCurrentValueTable);
		model.addAttribute("resourceTable", resourceTable);
		model.addAttribute("httpMetricsTable", httpMetricsTable);
		model.addAttribute("circuitBreakersTable", circuitBreakersTable);
		model.addAttribute("graphs", computeInstanceGraphs(instance,
				((Availability)service.getQoSSpecifications().get(Availability.class)).getMinThreshold(),
				((AverageResponseTime)service.getQoSSpecifications().get(AverageResponseTime.class)).getMaxThreshold(),
				service.getLatestAdaptationDate()));
		return "webpages/instanceDetails";
	}

	/* Display current status */
	@GetMapping("/adaptationStatus")
	public String adaptationStatus(Model model) {
		Map<String, List<AdaptationOption>> history = dashboardWebService.getChosenAdaptationOptionsHistory(adaptationHistorySize);
		Map<String, List<String>> historyTable = new HashMap<>();
		for (String serviceId : history.keySet()) {
			List<String> serviceHistory = new LinkedList<>();
			for (AdaptationOption option : history.get(serviceId))
				serviceHistory.add(option.getDescription() + "\nApplied at: " + option.getTimestamp());
			for (int i = serviceHistory.size(); i < adaptationHistorySize; i++)
				serviceHistory.add("");
			historyTable.put(serviceId, serviceHistory);
		}
		model.addAttribute("adaptationHistorySize", adaptationHistorySize);
		model.addAttribute("historyTable", historyTable);
		Modules activeModule = dashboardWebService.getActiveModule();
		model.addAttribute("activeModule", activeModule);
		Modules failedModule = dashboardWebService.getFailedModule();
		model.addAttribute("failedModule", failedModule);
		if (activeModule != null) {
			switch (activeModule) {
				case MONITOR -> model.addAttribute("statusDescription", "Monitor module is collecting metrics from the services.");
				case ANALYSE -> model.addAttribute("statusDescription", "Analyse module is computing the adaptation options from the metrics collected by the monitor.");
				case PLAN -> model.addAttribute("statusDescription", "Plan module is choosing the adaptation options to apply from the ones proposed by the Analyse module.");
				case EXECUTE -> model.addAttribute("statusDescription", "Execute module is applying the adaptation options chosen by the Plan module.");
			}
		}
		return "webpages/adaptationStatus";
	}

	private GraphData[] computeServiceGraphs(Service service) {
		GraphData[] graphs = new GraphData[2];
		// Values is ordered by timestamp ASC
		List<QoSHistory.Value> values;
		GraphData graph;
		int valuesSize, oldestValueIndex;

		graph = new GraphData("Timestamp (UTC)", "Availability", ((Availability)service.getQoSSpecifications().get(Availability.class)).getMinThreshold());
		values = service.getValuesHistoryForQoS(Availability.class);
		valuesSize = values.size();
		oldestValueIndex = maxHistorySize > valuesSize ? valuesSize-1 : maxHistorySize-1;
		for (int i = 0; i <= oldestValueIndex; i++) { // get only latest X values
			QoSHistory.Value v = values.get(oldestValueIndex-i);
			if (v.getTimestamp().before(service.getLatestAdaptationDate())) {
				graph.addPointBefore(v);
			} else {
				graph.addPointAfter(v);
			}
		}
		graph.generateAggregatedPoints();
		graphs[0] = graph;

		graph = new GraphData("Timestamp (UTC)", "Average Response Time [ms]", ((AverageResponseTime)service.getQoSSpecifications().get(AverageResponseTime.class)).getMaxThreshold());
		values = service.getValuesHistoryForQoS(AverageResponseTime.class);
		valuesSize = values.size();
		oldestValueIndex = maxHistorySize > valuesSize ? valuesSize-1 : maxHistorySize-1;
		for (int i = 0; i <= oldestValueIndex; i++) { // get only latest X values
			QoSHistory.Value v = values.get(oldestValueIndex-i);
			if (v.getTimestamp().before(service.getLatestAdaptationDate())) {
				graph.addPointBefore(v);
			} else
				graph.addPointAfter(v);
		}
		graph.generateAggregatedPoints();
		graphs[1] = graph;

		return graphs;
	}

	private GraphData[] computeInstanceGraphs(Instance instance, Double availabilityThreshold, Double artThreshold, Date serviceLatestAdaptationDate) {
		GraphData[] graphs = new GraphData[2];
		// Values is ordered by timestamp ASC
		List<QoSHistory.Value> values;
		GraphData graph;
		int valuesSize, oldestValueIndex;

		graph = new GraphData("Timestamp (UTC)", "Availability", availabilityThreshold);
		values = instance.getQoSCollection().getValuesHistoryForQoS(Availability.class);
		valuesSize = values.size();
		oldestValueIndex = maxHistorySize > valuesSize ? valuesSize-1 : maxHistorySize-1;
		for (int i = 0; i <= oldestValueIndex; i++) { // get only latest X values
			QoSHistory.Value v = values.get(oldestValueIndex-i);
			if (v.getTimestamp().before(serviceLatestAdaptationDate)) {
				graph.addPointBefore(v);
			} else
				graph.addPointAfter(v);
		}
		graph.generateAggregatedPoints();
		graphs[0] = graph;

		graph = new GraphData("Timestamp (UTC)", "Average Response Time [ms]", artThreshold);
		values = instance.getQoSCollection().getValuesHistoryForQoS(AverageResponseTime.class);
		valuesSize = values.size();
		oldestValueIndex = maxHistorySize > valuesSize ? valuesSize-1 : maxHistorySize-1;
		for (int i = 0; i <= oldestValueIndex; i++) { // get only latest X values
			QoSHistory.Value v = values.get(oldestValueIndex-i);
			if (v.getTimestamp().before(serviceLatestAdaptationDate)) {
				graph.addPointBefore(v);
			} else
				graph.addPointAfter(v);
		}
		graph.generateAggregatedPoints();
		graphs[1] = graph;

		return graphs;
	}


	/* Configuration page */
	@GetMapping("/configuration")
	public String configuration(Model model) {
		model.addAttribute("isAdaptationEnabled", dashboardWebService.isAdaptationEnabled());

		// MONITOR
		MonitorClient.GetInfoResponse monitorInfo = dashboardWebService.getMonitorInfo();
		model.addAttribute("monitorSchedulingPeriod", monitorInfo.getSchedulingPeriod());
		model.addAttribute("isMonitorRunning", monitorInfo.isRoutineRunning());

		// ANALYSE
		AnalyseClient.GetInfoResponse analyseInfo = dashboardWebService.getAnalyseInfo();
		model.addAttribute("metricsWindowSize", analyseInfo.getMetricsWindowSize());
		model.addAttribute("analysisWindowSize", analyseInfo.getAnalysisWindowSize());
		model.addAttribute("failureRateThreshold", analyseInfo.getFailureRateThreshold());
		model.addAttribute("unreachableRateThreshold", analyseInfo.getUnreachableRateThreshold());
		model.addAttribute("qosSatisfactionRate", analyseInfo.getQosSatisfactionRate());
		return "webpages/configuration";
	}

	// Monitor Configuration Endpoints
	@PostMapping("/configuration/changeMonitorSchedulingPeriod")
	public String changeMonitorSchedulingPeriod(Model model, @RequestParam(value = "monitorSchedulingPeriod") int monitorSchedulingPeriod) {
		dashboardWebService.changeMonitorSchedulingPeriod(monitorSchedulingPeriod);
		return configuration(model);
	}

	@PostMapping("/configuration/startMonitorRoutine")
	public String startMonitorRoutine(Model model) {
		dashboardWebService.startMonitorRoutine();
		return configuration(model);
	}

	@PostMapping("/configuration/stopMonitorRoutine")
	public String stopMonitorRoutine(Model model) {
		dashboardWebService.stopMonitorRoutine();
		return configuration(model);
	}

	// Analyse Configuration Endpoints
	@PostMapping("/configuration/changeMetricsWindowSize")
	public String changeMetricsWindowSize(Model model, @RequestParam(value = "metricsWindowSize") int metricsWindowSize) {
		dashboardWebService.changeMetricsWindowSize(metricsWindowSize);
		return configuration(model);
	}

	@PostMapping("/configuration/changeAnalysisWindowSize")
	public String changeAnalysisWindowSize(Model model, @RequestParam(value = "analysisWindowSize") int analysisWindowSize) {
		dashboardWebService.changeAnalysisWindowSize(analysisWindowSize);
		return configuration(model);
	}

	@PostMapping("/configuration/changeFailureRateThreshold")
	public String changeFailureRateThreshold(Model model, @RequestParam(value = "failureRateThreshold") double failureRateThreshold) {
		dashboardWebService.changeFailureRateThreshold(failureRateThreshold);
		return configuration(model);
	}

	@PostMapping("/configuration/changeUnreachableRateThreshold")
	public String changeUnreachableRateThreshold(Model model, @RequestParam(value = "unreachableRateThreshold") double unreachableRateThreshold) {
		dashboardWebService.changeUnreachableRateThreshold(unreachableRateThreshold);
		return configuration(model);
	}

	@PostMapping("/configuration/changeQoSSatisfactionRate")
	public String changeQoSSatisfactionRate(Model model, @RequestParam(value = "qosSatisfactionRate") double qosSatisfactionRate) {
		dashboardWebService.changeQoSSatisfactionRate(qosSatisfactionRate);
		return configuration(model);
	}

	// Adaptation start/stop
	@PostMapping("/configuration/startAdaptation")
	public String startAdaptation(Model model) {
		dashboardWebService.changeAdaptationStatus(true);
		return configuration(model);
	}
	@PostMapping("/configuration/stopAdaptation")
	public String stopAdaptation(Model model) {
		dashboardWebService.changeAdaptationStatus(false);
		return configuration(model);
	}

}
