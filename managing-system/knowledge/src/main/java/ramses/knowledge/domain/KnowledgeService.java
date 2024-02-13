package ramses.knowledge.domain;

import ramses.knowledge.domain.adaptation.options.AdaptationOption;
import ramses.knowledge.domain.adaptation.specifications.Availability;
import ramses.knowledge.domain.adaptation.specifications.AverageResponseTime;
import ramses.knowledge.domain.adaptation.specifications.QoSSpecification;
import ramses.knowledge.domain.adaptation.values.QoSCollection;
import ramses.knowledge.domain.adaptation.values.QoSHistory;
import ramses.knowledge.domain.architecture.Instance;
import ramses.knowledge.domain.architecture.InstanceStatus;
import ramses.knowledge.domain.architecture.Service;
import ramses.knowledge.domain.architecture.ServiceConfiguration;
import ramses.knowledge.domain.metrics.InstanceMetricsSnapshot;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import ramses.knowledge.domain.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Service
public class KnowledgeService {
    @Autowired
    private MetricsRepository metricsRepository;

    @Autowired
    private ConfigurationRepository configurationRepository;

    @Autowired
    private AdaptationChoicesRepository adaptationChoicesRepository;

    @Autowired
    private QoSRepository qosRepository;

    @Getter
    private final Map<String, Service> servicesMap = new ConcurrentHashMap<>();

    private Set<Instance> previouslyActiveInstances = new HashSet<>();

    // <serviceId, AdaptationOptions proposed by the Analyse>
    @Getter @Setter
    private Map<String, List<AdaptationOption>> proposedAdaptationOptions = new HashMap<>();

    // <serviceId, AdaptationOptions chosen by the Plan (in this implementation, the Plan chooses ONE option per service)>
    @Getter @Setter
    private Map<String, List<AdaptationOption>> chosenAdaptationOptions = new HashMap<>();

    @Getter
    private Modules activeModule = null;

    @Getter @Setter
    private Modules failedModule = null;


    public void setActiveModule(Modules activeModule) {
        this.activeModule = activeModule;
        if (activeModule == Modules.MONITOR) {
            // A new loop is started: reset the previous chosen options and the current proposed adaptation options
            for (String serviceId : chosenAdaptationOptions.keySet()) {
                servicesMap.get(serviceId).setLatestAdaptationDate(new Date());
            }
            proposedAdaptationOptions = new HashMap<>();
            chosenAdaptationOptions = new HashMap<>();
        }
    }

    public Date getLatestAdaptationDateForService(String serviceId) {
        return servicesMap.get(serviceId).getLatestAdaptationDate();
    }

    // Called by the KnowledgeInit
    public void addService(Service service) {
        servicesMap.put(service.getServiceId(), service);
    }
    

    public List<Service> getServicesList(){
        return servicesMap.values().stream().toList();
    }


    public void addMetricsFromBuffer(Queue<List<InstanceMetricsSnapshot>> metricsBuffer) {
        try {
            Set<Instance> shutdownInstancesStillMonitored = new HashSet<>();
            log.info("Saving new set of metrics");
            for (List<InstanceMetricsSnapshot> metricsList : metricsBuffer) {
                Set<Instance> currentlyActiveInstances = new HashSet<>();
                for (InstanceMetricsSnapshot metricsSnapshot : metricsList) {
                    Service service = servicesMap.get(metricsSnapshot.getServiceId());
                    //Skip the metricsSnapshot if it is not related to the current implementation
                    if (!Objects.equals(metricsSnapshot.getServiceImplementationId(), service.getCurrentImplementationId()))
                        continue;
                    Instance instance = service.getInstance(metricsSnapshot.getInstanceId());
                    log.info("INSTANCE FOUND:" + instance);
                    if (instance == null)
                        // If the instance has been shutdown, skip its metrics snapshot in the buffer. Next buffer won't contain its metrics snapshots.
                        throw new RuntimeException("Instance " +metricsSnapshot.getInstanceId()+" not found in service "+metricsSnapshot.getServiceId());
                    if (instance.getCurrentStatus() != InstanceStatus.SHUTDOWN) {
                        if (!instance.getLatestInstanceMetricsSnapshot().equals(metricsSnapshot)) {
                            metricsRepository.save(metricsSnapshot);
                            instance.setLatestInstanceMetricsSnapshot(metricsSnapshot);
                            instance.setCurrentStatus(metricsSnapshot.getStatus());
                        } else
                            log.warn("Metrics Snapshot already saved: " + metricsSnapshot);
                        if (metricsSnapshot.isActive() || metricsSnapshot.isUnreachable())
                            currentlyActiveInstances.add(instance);
                    } else {
                        shutdownInstancesStillMonitored.add(instance);
                    }
                }
                // Failure detection of instances
                if (!previouslyActiveInstances.isEmpty()) {
                    Set<Instance> failedInstances = new HashSet<>(previouslyActiveInstances);
                    failedInstances.removeAll(currentlyActiveInstances);
                    failedInstances.removeIf(instance -> instance.getCurrentStatus() == InstanceStatus.SHUTDOWN);
                    if (failedInstances.stream().anyMatch(instance -> instance.getCurrentStatus() == InstanceStatus.BOOTING)) {
                        log.error("Marking as FAILED instances that are BOOTING!");
                    }
                    // There should be only the instances that have been shutdown and are still monitored
                    failedInstances.forEach(instance -> {
                        instance.setCurrentStatus(InstanceStatus.FAILED);
                        InstanceMetricsSnapshot metrics = new InstanceMetricsSnapshot(instance.getServiceId(), instance.getInstanceId());
                        metrics.setStatus(InstanceStatus.FAILED);
                        metrics.applyTimestamp();
                        metricsRepository.save(metrics);
                        instance.setLatestInstanceMetricsSnapshot(metrics);
                    });
                }
                previouslyActiveInstances = new HashSet<>(currentlyActiveInstances);
            }
            // For each service, remove from the map of instances the instances that have been shutdown that are not monitored anymore
            for (Service service : servicesMap.values()) {
                Set<Instance> instancesToBeRemoved = new HashSet<>(service.getShutdownInstances());
                instancesToBeRemoved.removeAll(shutdownInstancesStillMonitored);
                for (Instance instance : instancesToBeRemoved) {
                    log.debug("{}: Removing shutdown instance {}", service.getServiceId(), instance.getInstanceId());
                    service.removeInstance(instance);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void markInstanceAsShutdown(String serviceId, String instanceId) {
        Service service = servicesMap.get(serviceId);
        Instance instance = service.getInstance(instanceId);
        InstanceMetricsSnapshot metrics = new InstanceMetricsSnapshot(instance.getServiceId(), instance.getInstanceId());
        metrics.setStatus(InstanceStatus.SHUTDOWN);
        metrics.applyTimestamp();
        metricsRepository.save(metrics);
        instance.setCurrentStatus(InstanceStatus.SHUTDOWN);
        instance.setLatestInstanceMetricsSnapshot(metrics);
    }

    public void changeServiceImplementation(String serviceId, String newImplementationId, List<String> newInstancesAddresses){
        Service service = servicesMap.get(serviceId);
        service.getCurrentImplementation().setPenalty(0);

        for (Instance instance : service.getInstances()) {
            markInstanceAsShutdown(serviceId, instance.getInstanceId());
            service.removeInstance(instance);
        }
        service.setCurrentImplementationId(newImplementationId);

        for (String instanceAddress : newInstancesAddresses) {
            service.createInstance(instanceAddress);
        }

        if (service.getConfiguration().getLoadBalancerType() == ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM) {
            Map<String, Double> newWeights = new HashMap<>();
            for(Instance instance : service.getInstances()){
                newWeights.put(instance.getInstanceId(), 1.0/service.getInstances().size());
            }
            setLoadBalancerWeights(serviceId, newWeights);
        }
    }

    public void addInstance(String serviceId, String instanceAddress){
        Service service = servicesMap.get(serviceId);
        service.createInstance(instanceAddress);
    }

    public InstanceMetricsSnapshot getMetrics(long id) {
        return metricsRepository.findById(id).orElse(null);
    }

    public void changeServicesConfigurations(Map<String, ServiceConfiguration> newConfigurations){
        for (String serviceId : newConfigurations.keySet()){
            Service service = servicesMap.get(serviceId);
            service.setConfiguration(newConfigurations.get(serviceId));
            configurationRepository.save(newConfigurations.get(serviceId));
        }
    }

    public List<InstanceMetricsSnapshot> getLatestNMetricsOfCurrentInstance(String serviceId, String instanceId, int n) {
        QoSCollection qosCollection = servicesMap.get(serviceId).getInstance(instanceId).getQoSCollection();
        QoSHistory.Value availabilityLatestValue = qosCollection.getQoSHistory(Availability.class).getLatestValue();
        System.out.println("LATEST AVAILABILITY VALUE: "+availabilityLatestValue);
        QoSHistory.Value artLatestValue = qosCollection.getQoSHistory(AverageResponseTime.class).getLatestValue();
        System.out.println("LATEST ART VALUE: "+artLatestValue);

        if (availabilityLatestValue == null)
            availabilityLatestValue = qosCollection.getQoSHistory(Availability.class).getCurrentValue();
        if (artLatestValue == null)
            artLatestValue = qosCollection.getQoSHistory(AverageResponseTime.class).getCurrentValue();
        if (availabilityLatestValue == null || artLatestValue == null)
            throw new RuntimeException("THIS SHOULD NOT HAPPEN");
        return metricsRepository.findLatestOfCurrentInstanceOrderByTimestampDesc(instanceId, artLatestValue.getTimestamp().after(availabilityLatestValue.getTimestamp()) ? artLatestValue.getTimestamp() : availabilityLatestValue.getTimestamp(), Pageable.ofSize(n)).stream().toList();
    }

    public List<InstanceMetricsSnapshot> getAllInstanceMetricsBetween(String instanceId, String startDateStr, String endDateStr) {
        Date startDate = Date.from(LocalDateTime.parse(startDateStr).toInstant(ZoneOffset.UTC));
        Date endDate = Date.from(LocalDateTime.parse(endDateStr).toInstant(ZoneOffset.UTC));
        return metricsRepository.findAllByInstanceIdAndTimestampBetween(instanceId, startDate, endDate).stream().toList();
    }

    public InstanceMetricsSnapshot getLatestByInstanceId(String instanceId) {
        return metricsRepository.findLatestByInstanceId(instanceId).stream().findFirst().orElse(null);
    }

    public List<InstanceMetricsSnapshot> getAllLatestByServiceId(String serviceId) {
        return metricsRepository.findLatestByServiceId(serviceId).stream().toList();
    }

    public Service getService(String serviceId) {
        return servicesMap.get(serviceId);
    }

    public List<AdaptationOption> getChosenAdaptationOptionsHistory(String serviceId, int n) {
        return adaptationChoicesRepository.findAllByServiceIdOrderByTimestampDesc(serviceId, Pageable.ofSize(n)).stream().toList();
    }

    public Map<String, List<AdaptationOption>> getChosenAdaptationOptionsHistory(int n) {
        return adaptationChoicesRepository.findAll(Pageable.ofSize(n)).stream().collect(Collectors.groupingBy(AdaptationOption::getServiceId));
    }

    public void proposeAdaptationOptions(Map<String, List<AdaptationOption>> proposedAdaptationOptions) {
        this.proposedAdaptationOptions = proposedAdaptationOptions;
        for (String serviceId : proposedAdaptationOptions.keySet()) {
            if (!proposedAdaptationOptions.get(serviceId).isEmpty()) {
                Service service = servicesMap.get(serviceId);
                service.getCurrentImplementation().incrementPenalty();
            }
        }
    }

    // Called by the Plan module to choose the adaptation options
    public void chooseAdaptationOptions(Map<String, List<AdaptationOption>> chosenAdaptationOptions) {
        this.chosenAdaptationOptions = chosenAdaptationOptions;
        this.chosenAdaptationOptions.values().forEach(serviceOptions -> {
            serviceOptions.forEach(option -> {
                option.applyTimestamp();
                adaptationChoicesRepository.save(option);
            });
        });
    }


    // Update QoS-related properties
    public void addNewInstanceQoSValue(String serviceId, String instanceId, Class<? extends QoSSpecification> qosClass, Double value, Date date) {
        servicesMap.get(serviceId).getInstance(instanceId).getQoSCollection().createNewQoSValue(qosClass, value, date);
    }

    public void addNewServiceQoSValue(String serviceId, Class<? extends QoSSpecification> qosClass, Double value, Date date) {
        servicesMap.get(serviceId).getCurrentImplementation().getQoSCollection().createNewQoSValue(qosClass, value, date);
    }

    public void updateServiceQoSCollection(String serviceId, QoSCollection qoSCollection) {
        servicesMap.get(serviceId).getCurrentImplementation().setQoSCollection(qoSCollection);
    }

    public void updateInstanceQoSCollection(String serviceId, String instanceId, QoSCollection qoSCollection) {
        servicesMap.get(serviceId).getInstance(instanceId).setQoSCollection(qoSCollection);
    }

    public void updateService(Service service) {
        servicesMap.put(service.getServiceId(), service);
    }

    public void updateBenchmark(String serviceId, String serviceImplementationId, String simpleClassName, Double value) {
        // concatena il nome della classe QoS e simpleClassName
        String qosSpecificationClassName = QoSSpecification.class.getPackage().getName() + "." + simpleClassName;
        // indica che la variabile qosClass può fare riferimento a una classe che estende QoSSpecification.
        Class<? extends QoSSpecification> qosClass;
        try {
            // < ? extends QoSSpecification > consente di lavorare con classi che estendono QoSSpecification in modo più generico.
            // specifica che la classe da castare deve essere una sottoclasse di QoSSpecification
            qosClass = (Class<? extends QoSSpecification>) Class.forName(qosSpecificationClassName);
            if (!QoSSpecification.class.isAssignableFrom(qosClass))
                throw new RuntimeException("The provided class " + qosClass.getName() + " does not extend the QoS class.");
            servicesMap.get(serviceId).getPossibleImplementations().get(serviceImplementationId).getQoSBenchmarks().put(qosClass, value);
            log.info("Updated "+simpleClassName+" benchmark for service " + serviceId + " of implementation " + serviceImplementationId + " to " + value);
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    public void setLoadBalancerWeights(String serviceId, Map<String, Double> weights) { // serviceId, Map<instanceId, weight>
        Service service = servicesMap.get(serviceId);
        ServiceConfiguration oldConfiguration = service.getConfiguration();
        ServiceConfiguration newConfiguration = new ServiceConfiguration();
        newConfiguration.setLoadBalancerType(oldConfiguration.getLoadBalancerType());
        newConfiguration.setLoadBalancerWeights(weights);
        newConfiguration.setServiceId(serviceId);
        newConfiguration.setCircuitBreakersConfiguration(oldConfiguration.getCircuitBreakersConfiguration());
        newConfiguration.setTimestamp(new Date());
        service.setConfiguration(newConfiguration);
        configurationRepository.save(service.getConfiguration());
    }

    public void updateServiceQosCollection(String serviceId,
                                           Map<String, Map<Class<? extends QoSSpecification>, QoSHistory.Value>> newInstancesValues,
                                           Map<Class<? extends QoSSpecification>, QoSHistory.Value> newServiceValues,
                                           Map<String, Map<Class<? extends QoSSpecification>, QoSHistory.Value>> newInstancesCurrentValues,
                                           Map<Class<? extends QoSSpecification>, QoSHistory.Value> newServiceCurrentValues) {
        Service service = servicesMap.get(serviceId);
        // Update the current value of all the instances of the service
        newInstancesCurrentValues.forEach((instanceId, newInstanceQoSCurrentValues) -> {
            Instance instance = service.getInstance(instanceId);
            newInstanceQoSCurrentValues.forEach((qosClass, qosValue) -> {
                instance.getQoSCollection().setCurrentValueForQoS(qosClass, qosValue);
            });
        });
        // Add the latest value of each QoS of all the instances of the service. Then persist it.
        newInstancesValues.forEach((instanceId, newInstanceQoSValues) -> {
            Instance instance = service.getInstance(instanceId);
            newInstanceQoSValues.forEach((qosClass, qosValue) -> {
                double threshold = -1;
                QoSSpecification qosSpecification = service.getQoSSpecifications().get(qosClass);
                if (qosClass.equals(Availability.class))
                    threshold = ((Availability) qosSpecification).getMinThreshold();
                else if (qosClass.equals(AverageResponseTime.class))
                    threshold = ((AverageResponseTime) qosSpecification).getMaxThreshold();
                instance.getQoSCollection().addNewQoSValue(qosClass, qosValue);
                qosRepository.save(new QoSValueEntity(serviceId, service.getCurrentImplementationId(), instanceId,
                        qosClass.getSimpleName(), threshold, instance.getCurrentValueForQoS(qosClass), qosValue));
            });
        });
        // Update the current value of each QoS for the service
        newServiceCurrentValues.forEach((qosClass, qosValue) -> {
            service.getCurrentImplementation().getQoSCollection().setCurrentValueForQoS(qosClass, qosValue);
        });
        // Add the latest value of each QoS for the service. Then persist it.
        newServiceValues.forEach((qosClass, qosValue) -> {
            double threshold = -1;
            QoSSpecification qosSpecification = service.getQoSSpecifications().get(qosClass);
            if (qosClass.equals(Availability.class))
                threshold = ((Availability) qosSpecification).getMinThreshold();
            else if (qosClass.equals(AverageResponseTime.class)) {
                threshold = ((AverageResponseTime) qosSpecification).getMaxThreshold();
                if (qosValue.getDoubleValue() > 5000)
                    log.warn("Huge ART for service " + serviceId);
            }
            service.getCurrentImplementation().getQoSCollection().addNewQoSValue(qosClass, qosValue);
            qosRepository.save(new QoSValueEntity(serviceId, service.getCurrentImplementationId(), null,
                    qosClass.getSimpleName(), threshold, service.getCurrentValueForQoS(qosClass), qosValue));
        });
    }

    // Useful methods to investigate the metrics of the instances

    public List<InstanceMetricsSnapshot> getAllInstanceMetrics(String instanceId) {
        return metricsRepository.findAllByInstanceId(instanceId).stream().toList();
    }

    public List<InstanceMetricsSnapshot> getAllMetricsBetween(String startDateStr, String endDateStr) {
        Date startDate = Date.from(LocalDateTime.parse(startDateStr).toInstant(ZoneOffset.UTC));
        Date endDate = Date.from(LocalDateTime.parse(endDateStr).toInstant(ZoneOffset.UTC));
        return metricsRepository.findAllByTimestampBetween(startDate, endDate).stream().toList();
    }

    public List<InstanceMetricsSnapshot> getNMetricsBefore(String instanceId, String timestampStr, int n) {
        Date timestamp = Date.from(LocalDateTime.parse(timestampStr).toInstant(ZoneOffset.UTC));
        return metricsRepository.findAllByInstanceIdAndTimestampBeforeOrderByTimestampDesc(instanceId, timestamp, Pageable.ofSize(n)).stream().toList();
    }

    public List<InstanceMetricsSnapshot> getNMetricsAfter(String instanceId, String timestampStr, int n) {
        Date timestamp = Date.from(LocalDateTime.parse(timestampStr).toInstant(ZoneOffset.UTC));
        return metricsRepository.findAllByInstanceIdAndTimestampAfterOrderByTimestampDesc(instanceId, timestamp, Pageable.ofSize(n)).stream().toList();
    }

    public InstanceMetricsSnapshot getLatestActiveByInstanceId(String instanceId) {
        return metricsRepository.findLatestOnlineMeasurementByInstanceId(instanceId).stream().findFirst().orElse(null);
    }


    public void invalidateQosHistory(String serviceId) {
        Service service = servicesMap.get(serviceId);
        service.getInstances().forEach(instance -> {
            instance.invalidateQoSHistory(Availability.class);
            instance.invalidateQoSHistory(AverageResponseTime.class);
        });
        service.invalidateQoSHistory(Availability.class);
        service.invalidateQoSHistory(AverageResponseTime.class);
        qosRepository.invalidateServiceQoSHistory(serviceId, service.getCurrentImplementationId());
    }

    public void updateImplementationPreference(String serviceId, String implementationId, double preference) {
        Service service = servicesMap.get(serviceId);
        service.getPossibleImplementations().get(implementationId).setPreference(preference);
    }

    public void updateAvailabilityThreshold(String serviceId, double availabilityThreshold) {
        Service service = servicesMap.get(serviceId);
        ((Availability)(service.getQoSSpecifications().get(Availability.class))).setMinThreshold(availabilityThreshold);
    }

    public void updateResponseTimeThreshold(String serviceId, double responseTimeThreshold) {
        Service service = servicesMap.get(serviceId);
        ((AverageResponseTime)(service.getQoSSpecifications().get(AverageResponseTime.class))).setMaxThreshold(responseTimeThreshold);
    }
}