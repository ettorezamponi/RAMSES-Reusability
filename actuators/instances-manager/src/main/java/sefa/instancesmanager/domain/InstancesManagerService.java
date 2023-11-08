package sefa.instancesmanager.domain;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
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
    private final DockerClient dockerClient;

    // <Profile, List of SimulationInstanceParams>
    private final Map<String, List<SimulationInstanceParams>> simulationInstanceParamsMap;


    public InstancesManagerService(Environment env, @Value("${CURRENT_PROFILE}") String currentProfile){
        this.env = env;
        localIp = getMachineLocalIp();
        dockerIp = env.getProperty("DOCKER_IP") != null ? env.getProperty("DOCKER_IP") : localIp;
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
            log.warn("\nContainer name: {} \n\tports: {}", Arrays.stream(container.getNames()).findFirst().orElse("N/A"), Arrays.toString(container.getPorts()));
        }

        //Container name: /sefa-delivery-proxy-1-service
        //ports: [ContainerPort(ip=0.0.0.0, privatePort=58095, publicPort=55025, type=tcp)]


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

        //simulation of addInstances with delivery proxy 2, attenzione che più di un istanza dello stesso container non si può avviare
        //addInstances("delivery-proxy-2-service", 1);

        //String delivery2port = env.getProperty("SERVER_PORT");
        // questo restituisce 58015, ovvero la porta dell'instance manager



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


    // addInstances(NAME, NO of INSTANCES)
    public List<ServiceContainerInfo> addInstances(String serviceImplementationName, int numberOfInstances) {
        String imageName = serviceImplementationName;
        List<ServiceContainerInfo> serviceContainerInfos = new ArrayList<>(numberOfInstances);
        List<SimulationInstanceParams> simulationInstanceParamsList;
        synchronized (lock) {
            if (serviceImplementationName.equalsIgnoreCase("restaurant-service") || serviceImplementationName.startsWith("payment-proxy"))
                simulationInstanceParamsList = simulationInstanceParamsMap.get(currentProfile);
            else
                simulationInstanceParamsList = List.of(new SimulationInstanceParams(0.0, 0.0, 0.0));
        }
        //hearth
        for (int i = 0; i < numberOfInstances; i++) {
            int randomPort = getRandomPort();
            //porta da richiamare dalle properties e non dichiarandola
            ExposedPort serverPort = ExposedPort.tcp(58096);
            Ports portBindings = new Ports();
            portBindings.bind(serverPort, Ports.Binding.bindIpAndPort("0.0.0.0", randomPort));
            List<String> envVars = buildContainerEnvVariables(randomPort, simulationInstanceParamsList.get(i % simulationInstanceParamsList.size()));
            // [EUREKA_IP_PORT=172.23.220.187:58082, API_GATEWAY_IP_PORT=172.23.220.187:58081, MYSQL_IP_PORT=172.23.220.187:3306,
            // SERVER_PORT=50916, HOST=172.23.220.187, SLEEP_MEAN=0.0, SLEEP_VARIANCE=0.0, EXCEPTION_PROBABILITY=0.0]
            String newContainerId = dockerClient.createContainerCmd("sbi98/sefa-"+imageName+":arm64")
                    .withName(imageName)
                    //.withEnv(envVars) NOT USEFUL !?!?
                    .withExposedPorts(serverPort)
                    .withHostConfig(newHostConfig().withPortBindings(portBindings).withNetworkMode("ramses-sas-net"))
                    .exec()
                    .getId();
            dockerClient.startContainerCmd(newContainerId).exec();
            log.debug("Container "+newContainerId+" started, with these specifics: \nenv var:"+envVars+"\nexposed ports: "+serverPort+"\nport bindings: "+portBindings.getBindings().toString()+"\nserver port: "+serverPort);
            serviceContainerInfos.add(new ServiceContainerInfo(imageName, newContainerId, imageName + "_" + randomPort, dockerIp, randomPort, envVars));

            List<Container> contain = dockerClient.listContainersCmd().exec();
            for (Container container : contain) {
                log.warn("\nContainer name: {} \n\tports: {}", Arrays.stream(container.getNames()).findFirst().orElse("N/A"), Arrays.toString(container.getPorts()));
            }
        }
        return serviceContainerInfos;
        // [ServiceContainerInfo(imageName=delivery-proxy-2-service, containerId=ab5b030b257b554088989c45ce6d68cf3164d52f5e5706adff23d35daa4b6f05,
        // containerName=delivery-proxy-2-service_51333, address=172.23.220.187, port=51333, envVars=[EUREKA_IP_PORT=172.23.220.187:58082,
        // API_GATEWAY_IP_PORT=172.23.220.187:58081, MYSQL_IP_PORT=172.23.220.187:3306, SERVER_PORT=51333, HOST=172.23.220.187, SLEEP_MEAN=0.0,
        // SLEEP_VARIANCE=0.0, EXCEPTION_PROBABILITY=0.0])]
    }

    public void startInstance(String serviceImplementationName, int port) {
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).withNameFilter(Collections.singleton(serviceImplementationName+"_"+port)).exec();
        if (containers.size() == 1) {
            Container container = containers.get(0);
            try {
                dockerClient.startContainerCmd(container.getId()).exec();
            } catch (NotFoundException|NotModifiedException e){
                log.warn("Cannot start container {}", container.getId());
            }
            return;
        } else if (containers.size() == 0){
            log.warn("Container {}_{} not found. Considering it as crashed.", serviceImplementationName, port);
            return;
        }
        throw new RuntimeException("Too many containers found: " + containers);
    }

    public void stopInstance(String serviceImplementationName, int port) {
        List<Container> containers = dockerClient.listContainersCmd().withNameFilter(Collections.singleton(serviceImplementationName+"_"+port)).exec();
        if (containers.size() == 1) {
            Container container = containers.get(0);
            try {
                dockerClient.stopContainerCmd(container.getId()).exec();
            } catch (NotFoundException|NotModifiedException e){
                log.warn("Container {} already removed", container.getId());
            }
            return;
        } else if (containers.size() == 0){
            log.warn("Container {}_{} not found. Considering it as crashed.", serviceImplementationName, port);
            return;
        }
        throw new RuntimeException("Too many containers found: " + containers);
    }

    private List<String> buildContainerEnvVariables(int serverPort, SimulationInstanceParams simulationInstanceParams) {
        List<String> envVars = new LinkedList<>();

        // Get Eureka, Gateway and MySQL addresses from Environment. When null, use the local IP address and the default ports
        String eurekaIpPort = env.getProperty("EUREKA_IP_PORT");
        //valore di eurekaIpPort se eurekaIpPort non è nullo, altrimenti viene utilizzato il valore di localIp concatenato con ":58082"
        envVars.add("EUREKA_IP_PORT=" + (eurekaIpPort == null ? localIp+":58082" : eurekaIpPort));
        String apiGatewayIpPort = env.getProperty("API_GATEWAY_IP_PORT");
        envVars.add("API_GATEWAY_IP_PORT="+(apiGatewayIpPort == null ? localIp+":58081" : apiGatewayIpPort));
        String mySqlIpPort = env.getProperty("MYSQL_IP_PORT");
        envVars.add("MYSQL_IP_PORT="+(mySqlIpPort == null ? localIp+":3306" : mySqlIpPort));
        envVars.add("SERVER_PORT="+serverPort);
        envVars.add("HOST="+dockerIp);
        envVars.add("SLEEP_MEAN="+simulationInstanceParams.getSleepDuration()*1000);
        envVars.add("SLEEP_VARIANCE="+simulationInstanceParams.getSleepVariance());
        envVars.add("EXCEPTION_PROBABILITY="+simulationInstanceParams.getExceptionProbability());
        //log.debug("*** BUILD CONTAINER ENV VARS: eurekaIpPort="+eurekaIpPort+", apiGatewayIpPort="+apiGatewayIpPort+", mySqlIpPort="+mySqlIpPort);

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
}

