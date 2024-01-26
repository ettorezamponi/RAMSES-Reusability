package sefa.instancesmanager.domain;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.*;
import java.util.*;

import static com.github.dockerjava.api.model.HostConfig.newHostConfig;


@Slf4j
@Service
public class InstancesManagerService {
    private final Object lock = new Object();
    private final Environment env;
    private String currentProfile;
    private final String localIp;
    private final String dockerIp;
    private final String arch;
    private final DockerClient dockerClient;

    // <Profile, List of SimulationInstanceParams>
    private final Map<String, List<SimulationInstanceParams>> simulationInstanceParamsMap;


    public InstancesManagerService(Environment env, @Value("${CURRENT_PROFILE}") String currentProfile){
        this.env = env;
        localIp = getMachineLocalIp();
        dockerIp = env.getProperty("DOCKER_IP") != null ? env.getProperty("DOCKER_IP") : localIp;
        arch = env.getProperty("ARCH") != null ? env.getProperty("ARCH") : "arm64";
        String dockerPort = env.getProperty("DOCKER_PORT");
        if (dockerIp == null || dockerIp.isEmpty() || dockerPort == null || dockerPort.isEmpty())
            throw new RuntimeException("Docker IP and port must be set");
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://"+dockerIp+":"+dockerPort)
                .build();
        log.warn("Docker host: {}", config.getDockerHost());
        this.currentProfile = currentProfile;
        simulationInstanceParamsMap = new HashMap<>();
        dockerClient = DockerClientBuilder.getInstance(config).build();
        List<Container> containers = dockerClient.listContainersCmd().exec();
        for (Container container : containers) {
            log.warn("\nContainer name: {} \nports: {}", Arrays.stream(container.getNames()).findFirst().orElse("N/A"), Arrays.toString(container.getPorts()));
        }

        //Container name: /sefa-delivery-proxy-1-service
        //	ports: [ContainerPort(ip=0.0.0.0, privatePort=58095, publicPort=55025, type=tcp)]


        /*for (Image container : containers) {
            log.warn("\nImage label: "+container.getId() + ", REPO DIGEST: " + Arrays.toString(container.getRepoTags()));
        }/*
        // Image label: sha256:a7b94a512d1a 7f6a1d08d90cfe10975e783c506765588105c6a5f6fdeaef45fd,
        // REPO DIGEST: [sbi98/sefa-payment-proxy-2-service:arm64]

        InspectContainerResponse analyse = dockerClient.inspectContainerCmd("sefa-web-service").exec();
        log.debug("Container ispezionato: " +analyse.getName());

        /*String newContainerId = dockerClient.createContainerCmd("delivery-proxy-2-service:arm64")
                .withName("delivery-2-FINALLY")
                .exec()
                .getId();*/
        //dockerClient.startContainerCmd("delivery-proxy-2-service").exec();
        /*String imageName = "sefa-delivery-proxy-2-service";
        String newContainerId = dockerClient.createContainerCmd(imageName)
                .withName(imageName)
                .exec()
                .getId();
        dockerClient.startContainerCmd(newContainerId).exec();
        log.debug("**** START NEW IMAGE ****");*/

        //addInstances("teastore-webui", 1);
        //stopInstance("auth", 8080);
        addInstances("teastore-image", 1);

        //TODO CRASHA SEMPRE A MENO CHE NON FACCIAMO RIPARTIRE IL VECCHIO CONTAINER

        switch (currentProfile) {
            case "PerfectInstance" -> simulationInstanceParamsMap.put(currentProfile, List.of(
                    // (failureRate, sleepDuration, sleepVariance)
                    new SimulationInstanceParams(0.0, 0.01, 0.01)
            ));
            case "SlowInstance100ms" -> simulationInstanceParamsMap.put(currentProfile, List.of(
                    // (failureRate, sleepDuration, sleepVariance)
                    new SimulationInstanceParams(0.0, 0.1, 0.02)
            ));
            case "aBitFaultyInstance" -> simulationInstanceParamsMap.put(currentProfile, List.of(
                    // (failureRate, sleepDuration, sleepVariance)
                    new SimulationInstanceParams(0.02, 0.01, 0.001)
            ));
            case "AverageFaultyInstance" -> simulationInstanceParamsMap.put(currentProfile, List.of(
                    // (failureRate, sleepDuration, sleepVariance)
                    new SimulationInstanceParams(0.04, 0.02, 0.001)
            ));
            case "FaultyInstance" -> simulationInstanceParamsMap.put(currentProfile, List.of(
                    // (failureRate, sleepDuration, sleepVariance)
                    new SimulationInstanceParams(0.85, 0.015, 0.001)
            ));
            default -> {
            }
        }
    }


    public List<ServiceContainerInfo> addInstances(String serviceImplementationName, int numberOfInstances) {
        //String imageName = "giamburrasca/sefa-"+serviceImplementationName+":"+arch;
        String imageName = serviceImplementationName;
        List<ServiceContainerInfo> serviceContainerInfos = new ArrayList<>(numberOfInstances);
        List<SimulationInstanceParams> simulationInstanceParamsList;
        Boolean special = false;

        //per il registry abbiamo bisogno di avere lo stesso nome e così funziona
        String containerName = serviceImplementationName.split("-")[1];
        log.info("CONTAINER NAME: " + containerName);

        if (containerName.contains("registry") || containerName.contains("persistence") || containerName.contains("recommender") || containerName.contains("image")) {
            special = true;
        }

        // Registry should have the same name, so completly delete the previous one
        //TODO theoretically persistence service could have more than one implementation
        if (special) {
            if (numberOfInstances > 1) {
                numberOfInstances = 1;
            }
            deleteContainer(containerName);
            log.info("Deleted {} container", containerName);
        }

        synchronized (lock) {
            if (serviceImplementationName.equalsIgnoreCase("restaurant-service") || serviceImplementationName.startsWith("payment-proxy"))
                simulationInstanceParamsList = simulationInstanceParamsMap.get(currentProfile);
            else
                simulationInstanceParamsList = List.of(new SimulationInstanceParams(0.0, 0.0, 0.0));
        }
        for (int i = 0; i < numberOfInstances; i++) {
            int randomPort = getRandomPort();
            String dockerName;
            ExposedPort exposedRandomPort = ExposedPort.tcp(8080); // intern port of the container
            Ports portBindings = new Ports();
            portBindings.bind(exposedRandomPort, Ports.Binding.bindIpAndPort("0.0.0.0", randomPort));

            //String containerName = "sefa-" + serviceImplementationName + "-" + randomPort;
            //String containerName = serviceImplementationName + "-" + randomPort;

            if (special)
                dockerName = containerName;
            else
                dockerName = containerName + "-" + randomPort;

            HostConfig hostConfig = new HostConfig();
            hostConfig.withPortBindings(portBindings);
            hostConfig.withNetworkMode("teastore");
            List<String> envVars = buildContainerEnvVariables(containerName, randomPort, simulationInstanceParamsList.get(i % simulationInstanceParamsList.size()));
            String newContainerId = dockerClient.createContainerCmd(imageName)
                    .withImage(imageName)
                    .withName(dockerName)
                    .withEnv(envVars)
                    .withExposedPorts(exposedRandomPort)
                    .withHostConfig(hostConfig)
                    .exec()
                    .getId();
            dockerClient.startContainerCmd(newContainerId).exec();
            serviceContainerInfos.add(new ServiceContainerInfo(imageName, newContainerId, containerName, containerName, randomPort, envVars));
        }
        return serviceContainerInfos;
    }

    public void startInstance(String address, int port) {
        List<Container> containers = dockerClient
                .listContainersCmd()
                .withShowAll(true)
                .exec()
                .stream()
                .filter(container ->
                        Arrays.stream(container.getNames()).anyMatch(name -> name.contains(address))
                )
                .toList();

        if (containers.size() == 1) {
            Container container = containers.get(0);
            try {
                dockerClient.startContainerCmd(container.getId()).exec();
            } catch (NotFoundException|NotModifiedException e){
                log.warn("Cannot start container {}", container.getId());
            }
            return;
        } else if (containers.size() == 0) {
            log.warn("Container {} at port {} not found. Considering it as crashed.", address, port);
            return;
        }
        throw new RuntimeException("Too many containers found to start: " + containers);
    }

    public void stopInstance(String address, int port) {
        List<Container> containers = dockerClient
                .listContainersCmd()
                .exec()
                .stream()
                .filter(container ->
                        Arrays.stream(container.getNames()).anyMatch(name -> name.contains(address))
                )
                .toList();

        if (containers.size() == 1) {
            Container container = containers.get(0);
            try {
                dockerClient.stopContainerCmd(container.getId()).exec();
            } catch (NotFoundException|NotModifiedException e){
                log.warn("Container {} already removed", container.getId());
            }
            return;
        } else if (containers.size() == 0) {
            log.warn("Container {} at port {} not found. Considering it as crashed.", address, port);
            return;
        }

        if (containers.size() > 1) {

            // check if the port is inside the container's name
            // TODO it will be very difficult to retrieve two containers with same randomly numbers inside the name
            List<Container> containersWithPort = containers
                    .stream()
                    .filter(container ->
                        Arrays.stream(container.getNames()).anyMatch(name -> name.matches(String.valueOf(port))))
                    .toList();

            // if we found a container without the port inside the name, it will be the first started, so the last one
            if (containersWithPort.isEmpty()) {
                Container lastContainer = containers.get(containers.size() - 1);
                dockerClient.stopContainerCmd(lastContainer.getId()).exec();
                log.info(" WE STOPPED THE ORIGINAL CONTAINER");
                return;

            //otherwise we stop the container with the number inside the name
            } else {
                Container containerWithPortToStop = containersWithPort.get(0);
                dockerClient.stopContainerCmd(containerWithPortToStop.getId()).exec();
                log.info("WE STOPPED THE CONTAINER WITH THE NUMBER {} INSIDE THE NAME", port);
                return;
            }
        }

        throw new RuntimeException("Too many containers found with same attributes: " + containers);
    }

    private List<String> buildContainerEnvVariables(String containerName, int serverPort, SimulationInstanceParams simulationInstanceParams) {
        List<String> envVars = new LinkedList<>();

        // Get Eureka, Gateway and MySQL addresses from Environment. When null, use the local IP address and the default ports
        /* String eurekaIpPort = env.getProperty("EUREKA_IP_PORT");
        envVars.add("EUREKA_IP_PORT=" + (eurekaIpPort == null ? localIp+":58082" : eurekaIpPort));
        String apiGatewayIpPort = env.getProperty("API_GATEWAY_IP_PORT");
        envVars.add("API_GATEWAY_IP_PORT="+(apiGatewayIpPort == null ? localIp+":58081" : apiGatewayIpPort));
        String mySqlIpPort = env.getProperty("MYSQL_IP_PORT");
        envVars.add("MYSQL_IP_PORT="+(mySqlIpPort == null ? localIp+":3306" : mySqlIpPort)); */
        if (!containerName.contains("registry")) {
           envVars.add("REGISTRY_HOST=" + env.getProperty("REGISTRY_HOST"));
        }
        if (containerName.contains("persistence"))
            envVars.add("DB_HOST="+env.getProperty("DB_HOST"));
        envVars.add("SERVER_PORT="+serverPort);
        envVars.add("HOST_NAME="+containerName);
        envVars.add("SLEEP_MEAN="+simulationInstanceParams.getSleepDuration()*1000);
        envVars.add("SLEEP_VARIANCE="+simulationInstanceParams.getSleepVariance());
        envVars.add("EXCEPTION_PROBABILITY="+simulationInstanceParams.getExceptionProbability());
        return envVars;
    }

    private int getRandomPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            if (serverSocket.getLocalPort() == 0)
                throw new IOException();
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("Could not find a free TCP/IP port to start the server", e);
        }
    }

    public void changeProfile(String newProfile) {
        synchronized (lock) {
            currentProfile = newProfile;
        }
    }

    private String getMachineLocalIp() {
        try (Socket socket = new Socket("1.1.1.1", 80)) {
            InetSocketAddress addr = (InetSocketAddress) socket.getLocalSocketAddress();
            log.info("Local address: {}", addr.getAddress().getHostAddress());
            return addr.getAddress().getHostAddress();
        } catch (IOException e) {
            throw new RuntimeException("Impossible to get local IP address", e);
        }
    }

    private void deleteContainer(String containerToDelete) {
        try {
            //TODO capire se questo ulteriore controllo può esser utile oppure crea problemi
            //if(!dockerClient.inspectContainerCmd("registry").exec().getState().getRunning()){}
            dockerClient.removeContainerCmd(containerToDelete).withForce(true).exec();
            log.info("{} container removed after crash to be able to instantiate a new one!", containerToDelete);
        } catch (NotFoundException|NotModifiedException e){
            log.warn("Error removing 'REGISTRY container' \nWith the following error: " + e);
        }
    }

}
