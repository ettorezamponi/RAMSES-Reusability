# RAMSES - ICSE2024 presentation
MSc final thesis project by Ettore Zamponi.

We present four scenarios that the actuator is able to recognise and apply to SEFA (application composed of microservices built ad-hoc).

In these scenarios, before each experiment is executed, the Managed System is freshly de-ployed with one nominal instance per service (i.e., without any manipulation).

Remember to clean the config-server on GitHub, after each adaptation, new weighted values for the adapted services are pushed into the application.properties file. Otherwise, when a new deployment of RAMSES is attempted, knowledge will find configuration incompatibilities and will not be able to be executed.

N.B. Pay attention to the github env var to be able to push on the correct config server!

## Development Ambient
Together with the actual code of both RAMSES and SEFA, we also provide a set of ready-to-use docker scenarios. By following the next steps, you can set up and run both systems on the same machine. 

To begin with, install [Docker](https://www.docker.com/) on your machine and run it. After the installation, we suggest to configure it with the following minimum requirements:
- **CPU**: 8
- **Memory**: 10GB
- **Swap**: 1GB

The whole Self-Adaptive System was developed, run and tested on a 2023 Apple MacBook Pro with the following specifications:
- **SoC**: Apple M2 Pro (10-core CPU, 16-core GPU)
- **RAM**: 16GB LPDDR4
- **Storage**: 512GB on NVMe SSD
- **OS**: macOS Sonoma 14.1.1
- **IDE**: Intellij IDEA
- **Docker** v24.0.6 (allocating 9 CPUs, 10GB Memory, 2GB Swap)

The **Java** version used by the project is version `16.0.2`.

## Installation guide

1. ### Create the configuration repo
	The next step involves the creation of a GitHub repository (if you don’t have one yet) to be used by the _Managed System Config Server_ as the configuration 	repository. You can do so by forking [our repository](https://github.com/ramses-sas/config-server). Check that the `application.properties` file does not 	include any load balancer weight. If so, simply delete those lines and push on your repository. Once you have created your configuration repository, create 	an environmental variable storing its URL by running the following command, after replacing `<YOUR_REPO_URL>` with the URL of the repository you just 		created:
	```
	$ export GITHUB_REPOSITORY_URL=<YOUR_REPO_URL>
	```
	The `GITHUB_REPOSITORY_URL` variable should look like `https://github.com/ramses-sas/config-server.git`
	
	Now, generate a GitHub personal access token to grant the _Managed System_ the permission to push data on your repository. You can do so by following [this 	guide](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token).
	Once again, create an environmental variable storing your access token by running the following command, after replacing `<YOUR_TOKEN>` with the token you 	just created:
	```
	$ export GITHUB_OAUTH=<YOUR_TOKEN> 
	```
	The `GITHUB_OAUTH` variable should look like an alphanumeric string.

2. ### Launch the socat command

	Launch the port forwarding command through Socat (or similar) as explained [here](#Troubleshooting-and-Known-Issues).

3. ### Launch the script
   
   	The bash script to execute the "ready-to-use" system is the [SETUP_ICSE.sh](/bash_scripts/execute/SETUP_ICSE.sh).

## Scenarios

* ### Scenario 1 - *handleAddInstanceOption*

  Simulating a crash or connection problem as a real case, we make one of the microservices unreachable by forcing its shutdown.
  The managing will realise this situation, and will start (forced option) a new instance of the same microservice in order to resume correct execution of the system as soon as possible.

  To do this, a *failure injection* implemented in the simulation (lasting 5 minutes) [rest-client](./managed-system/rest-client/src/main/java/sefa/restclient/domain/FailureInjectionService.java) code is used.
  In this case, it was decided to crash the 'sefa-restaurant-service' 180 seconds after the start modifying the following variables inside the [application.properties](./managed-system/rest-client/src/main/resources/application.properties).
  ```
  FAILURE_INJECTION = Y
  FAILURE_INJECTION_1_START = 180
  ID_OF_INSTANCE_TO_FAIL = restaurant-service@sefa-restaurant-service:58085
  ```

  Through the *plan* logs we see that a service is unreachable and the addition of a new service will be forced:
  ```
  Instances: restaurant-service@sefa-restaurant-service:58085
  Instance failed or unreachable
  
  FORCED - Add a new instance. Service: RESTAURANT-SERVICE No instances available
  ```

  The blue line represents the *restaurant service* before the adaptation, and the green line the after, i.e. after forcibly adding the new instance.
  ![alt](./documents/plotScenari/scenario1.png)

  Let it be clear that this can be done with all instances of SEFA and after any time.

  In addition, the *ordering service* tends to have a response time of approximately 1000-1200 ms, so during the simulation we can also see the addition (non-forced option) of an instance of the *ordering service* in order to observe the threshold.
  ```
  ORDERING-SERVICE: Selected option AddInstanceOption for AverageResponseTime with benefit 1.507561964285497.
  Details: Goal: AverageResponseTime - Add a new instance. Service: ORDERING-SERVICE The service avg response time specification is not satisfied
  ```
  This adaptation depends also on the randomness of the simulation, so it is not certain that it always happens in every simulation.


* ### Scenario 2 - *handleChangeImplementationOption*

  This represents the most complete scenario in which the managing recognises a better implementation, stops the old container and starts the new one with higher benefits.

  At the beginning of the simulation, a very low Average Response Time threshold (150) is set such that it cannot be met by the *delivery service* instance, through the [application.properties](./managed-system/rest-client/src/main/resources/application.properties) of the simulation:
  ```
  CHANGE_THRESHOLD=Y
  MAX_ART_THRESHOLD=150
  CHANGE_THRESHOLD_START=10
  CHANGE_THRESHOLD_DURATION=120
  DEFAULT_THRESHOLD=500
  ```
  
  As we can see in the following plot where the green lines (*delivery-proxy-1-service*) is over the threshold:

  ![alt](./documents/plotScenari/scenario2-1.png)

  The managing notices the problem, and change the implementation seeing that *delivery-proxy-2-service* has much better values than the 1, which is not working well.
  
  These are the configuration of the three different potential instances of the *delivery-proxy-service* inside the configuration file [system_architecture.json](./managing-system/knowledge/architecture_sla/sefa/system_architecture.json):
  ```
  "service_id":"DELIVERY-PROXY-SERVICE",
	"implementations" : [
		{
			"implementation_id" : "delivery-proxy-1-service",
			"implementation_trust" : 1,
			"preference" : 0.2,
			"instance_load_shutdown_threshold" : 0.4
		},
		{
			"implementation_id" : "delivery-proxy-2-service",
			"implementation_trust" : 9,
			"preference" : 0.6,
			"instance_load_shutdown_threshold" : 0.4
		},
		{
			"implementation_id" : "delivery-proxy-3-service",
			"implementation_trust" : 7,
			"preference" : 0.2,
			"instance_load_shutdown_threshold" : 0.4
		}
	]
  ```
  The simulation lasts 5 minutes and the plan's logs will show the correct change implementation adaptation executed:
  ```
  DELIVERY-PROXY-SERVICE: Selected option ChangeImplementationOption for AverageResponseTime with benefit 1.0760645103065511. 
  Details: Goal: AverageResponseTime - Change DELIVERY-PROXY-SERVICE implementation from delivery-proxy-1-service to delivery-proxy-2-service. Changing implementation
  ```
  At this point the correct Average Response Time threshold will be restored to a normal value (in other cases, RAMSES will continue to allocate container to respect the unfeasible threshold) and the new implementation could satisfy it.
  
  ![alt](./documents/plotScenari/scenario2-2.png)

* ### Scenario 3 - *handleChangeLBWeightsOption*

  This particular action is performed whenever there is an adaptation, in particular whenever a new service is allocated in aid of the previous ones already in place.
  In particular, the *change load balancer weight* takes care of distributing the workload between the different instances of the same service.

  Managing and managed were deployed normally. During the simulation (10 minutes) the Average Response Time of the ordering-service is raised twice, after 90 seconds and after 180 seconds from the start of the simulation, settings its value in the [application.properties](./managed-system/rest-client/src/main/resources/application.properties) file.
  ```
  FAKE_SLOW_ORDERING=Y
  
  FAKE_SLOW_ORDERING_1_SLEEP=1000
  FAKE_SLOW_ORDERING_2_SLEEP=600
  
  FAKE_SLOW_ORDERING_1_START=90
  FAKE_SLOW_ORDERING_1_DURATION=60
  FAKE_SLOW_ORDERING_2_START=180
  FAKE_SLOW_ORDERING_2_DURATION=60
  ```

  By doing so, the managing will start another *ordering service* in addition to the existing ones, and the load weight balancing function will take care of splitting the work and, very importantly, save any new weight changes in the GitHub repository used by the configuration server.
  As seen below, the result in the configuration repo and in the *plan* service logs:

  ```
  loadbalancing.ordering-service.sefa-ordering-service-35359_35359.weight=0.8
  loadbalancing.ordering-service.sefa-ordering-service_58086.weight=0.2
  ```
  ```
  Details: Goal: AverageResponseTime - Change LBW. Service: ORDERING-SERVICE
  New weights are: {ordering-service@sefa-ordering-service:58086=0.2, ordering-service@sefa-ordering-service-35359:35359=0.8}.
  At least one instance satisfies the avg Response time specifications
  ```

  Configurations are saved each time they are changed, so that each time the managing is restarted, the changes are retrieved during the knowledge start-up. It is therefore important to clean up the configuration repo each time you want to re-deploy a clean managing system.

  The plot of the *ordering service* show the benefits of, firstly instantiate a new service when the first one is slowed (first peak), and then changing the load balancing weights when the first instance is slowed again entrusting more work to the instance that performs better (second peak, color green):

  ![alt](./documents/plotScenari/scenario3.png)


* ### Scenario 4 - *handleShutdownInstanceOption*

  In this last scenario we see the last of the adaptation options that RAMSES is able to implement, namely *handleShutdownInstanceOption*.

  We create a simulation (lasting 10 minutes) where after 90 seconds from the start we deliberately slow down the *ordering service* for one minute (after which it will be reset to normal values), this will cause the managing to start a new instance to help our slowed down service. Always through the [application.properties](./managed-system/rest-client/src/main/resources/application.properties).

  ```
  FAKE_SLOW_ORDERING=Y
  FAKE_SLOW_ORDERING_1_SLEEP=1000
  FAKE_SLOW_ORDERING_1_START=90
  FAKE_SLOW_ORDERING_1_DURATION=60
  ```

  After this, the workload will be handled by two instances that will divide the workload in half by setting weights (as seen in the previous scenario) resulting in the configuration repo as follows:
  
  ```
  loadbalancing.ordering-service.sefa-ordering-service-42315_42315.weight=0.5
  loadbalancing.ordering-service.sefa-ordering-service_58086.weight=0.5
  ```

  At this point, after 260 seconds from the beginning, it causes one of the two instances to decrease its availability of the 28%, so that the average availability of the entire ordering-service straddles the threshold.
  In this way, the managing will have to take note of the unfollowed threshold and act accordingly, in other words add another instances or change the load balancing weights again.
  
  The managing, probably would change the load balancing weights assigning more work to the instance that performs best and has not been slowed down.

  After 400 seconds it will do the same thing again by slowing down the same instance as before (the one with a lower workload).
  
  Another times through the same properties file:

  ```
  FAKE_EXCEPTION_ORDERING=Y
  FAKE_EXCEPTION_VALUE_1=0.28
  FAKE_EXCEPTION_VALUE_2=0.7
  FAKE_EXCEPTION_START_1=260
  FAKE_EXCEPTION_START_2=400
  ```

  Finally, the managing will realise that the threshold has not been met and will change the weights of the two instances (*plan*'s logs), reducing the workload of the least available instance to zero and thus shutting it down definitively.

  ```
  Executing adaptation option: Goal: Availability - Change LBW. Service: ORDERING-SERVICE
  New weights are: {ordering-service@sefa-ordering-service-42315:42315=1.0}.
  SHUTDOWN INSTANCE REQUEST: serviceId= ORDERING-SERVICE, instanceToShutdownId=ordering-service@sefa-ordering-service:58086
  ```
  And consequently updating the configuration repo in this way:

   ```
   loadbalancing.ordering-service.sefa-ordering-service-42315_42315.weight=1.0
   ```
  
  The availability plot of the *ordering service* clearly shows the benefits of weight changes occurring twice, when the availability goes under the threshold:
  ![alt](./documents/plotScenari/scenario4.png)

  If the availability is very close or straddles the threshold, it is more likely that the weights will be changed, otherwise, if there is a large gap between the actual availability and the threshold, a new instance will be added to help the two already present.

  In this same simulation, it happened to see both a change of weights as desired and the addition of a new instance. This is due to the random part of the simulation, fake purchases and various mathematical calculations which even in a real scenario cannot be controlled.

# Troubleshooting and Known Issues

1) A known issue on macOS involves the Actuator component, that sometimes cannot directly contact the Docker interface to run or stop containers. This results in the Instances Manager container to fail its booting process. To solve this issue, install socat using [this guide](https://stackoverflow.com/questions/16808543/install-socat-on-mac) and run the command:
   ```
   $ socat -d TCP-LISTEN:2375,range=0.0.0.0/0,reuseaddr,fork UNIX:/var/run/docker.sock
   ```
	If the path is not correct for the Docker configuration, follow this [forum question](https://forums.docker.com/t/is-a-missing-docker-sock-file-a-bug/134351) about.
   
2) Clean the *application.properties* file in the configuration server repo on GitHub after each adaptation.
3) Sometimes the Knowledge container is not able to find correctly all the service. Just try to restart the knowledge container, and only after its start-up, relaunch all remaining managing containers.
   This is a problem of the Discovery Client not fast enough to register or unregister services. [SOLVED]
