# RAMSES - ICSE2024 presentation
MSc final thesis project by Ettore Zamponi.

We present four scenarios that the actuator is able to recognise and apply to SEFA (application composed of microservices built ad-hoc).

In these scenarios, before each experiment is executed, the Managed System is freshly de-ployed with one nominal instance per service (i.e., without any manipulation).

Remember to clean the config-server on GitHub, after each adaptation, new weighted values for the adapted services are pushed into the application.properties file. Otherwise, when a new deployment of RAMSES is attempted, knowledge will find configuration incompatibilities and will not be able to be executed.

PAY ATTENTION TO THE GITHUB ENV VAR TO BE ABLE TO PUSH ON THE CORRECT CONFIG SERVER!

* ## Scenario 1 - *handleAddInstanceOption*

  Simulating a crash or connection problem as a real case, we make one of the microservices unreachable by forcing its shutdown.
  The managing will realise this situation, and will start (forced option) a new instance of the same microservice in order to resume correct execution of the system as soon as possible.

  To do this, a *failure injection* implemented in the [rest-client](./managed-system/rest-client/src/main/java/sefa/restclient/domain/FailureInjectionService.java) code is used.
  In this case, it was decided to crash the 'sefa-restaurant-service' 90 seconds after the start of the simulation modifying the following variables inside the [application.properties](./managed-system/rest-client/src/main/resources/application.properties).
  ```
  FAILURE_INJECTION = Y
  FAILURE_INJECTION_1_START = 90
  ID_OF_INSTANCE_TO_FAIL = restaurant-service@sefa-restaurant-service:58085
  ```
  Let it be clear that this can be done with all instances of SEFA and after any time. In addition, it is also possible to restart the same instance after a certain amount of time.

  In the same simulation, after 9 minutes, it happens that the managing decides to start a new instance (non-forced) of the "sefa-ordering-service" microservice. This is done in order to work in a more optimised way and have a much lower ART dividing the workload.

  -----------

  sefa slowing sleep mean - non forced adding instance
  ```
  FAKE_SLOW_ORDERING_1_START=90
  FAKE_SLOW_ORDERING_1_DURATION=45
  ```


* ## Scenario 2 - *handleChangeImplementationOption*

  This represents the most complete scenario in that the managing recognises a better implementation, stops the old container and starts the new one with higher benefits.
  
  This was done modifiyng the environment as follows to simulate a situation in which one of the services is unable to meet the specified thresholds.
  The ART threshold for 'DELIVERY-PROXY-SERVICE' is lowered to 150 in the [qos.json](./managing-system/knowledge/architecture_sla/sefa/qos_specification.json):
  ```
  "service_id" : "DELIVERY-PROXY-SERVICE",
	"qos" : [
			{
				"name" : "availability",
				"weight" : 0.5,
				"min_threshold" : 0.92
			},
			{
				"name" : "average_response_time",
				"weight" : 0.5,
				"max_threshold": 150
			}
	]
  ```
  The 'delivery-proxy-2-service' benchmark was lowered to encourage the system to choose that particular service as a substitute in the [system_benchmarks.json](./managing-system/knowledge/architecture_sla/sefa/system_benchmarks.json):
  ```
  "implementation_id" : "delivery-proxy-2-service",
	"adaptation_benchmarks" : [
			{
				"name": "average_response_time",
				"benchmark": 150
			},
			{
				"name": "availability",
				"benchmark": 0.94
			}
		]
  ```
  And finally, the [system_architecture.json](./managing-system/knowledge/architecture_sla/sefa/system_architecture.json) was slightly modified for demonstration purposes, to encourage people to choose the 'delivery-proxy-2-service' over the other two possible implementations:
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
  At this point a 10-minute simulation is run and a *changeImplementation* will take place due to the threshold not being met by "delivery-proxy-1-service".

  Finally, it will inject a feasible threshold to return the situation to normal declaring in the [application.properties](./managed-system/rest-client/src/main/resources/application.properties) using the method *updateMaxThreshold* implemented inside the [rest-client](.managed-system/rest-client/src/main/java/sefa/restclient/domain/BenchmarksChangerService.java):
  ```
  CHANGE_THRESHOLD=Y
  MAX_ART_THRESHOLD=350
  CHANGE_THRESHOLD_START=105
  ```
  In other cases,  RAMSES will continue to allocate container to respect the unfeasible threshold.

* ## Scenario 3 - *handleChangeLBWeightsOption*

  This particular action is performed whenever there is an adaptation, in particular whenever a new service is allocated in aid of the previous ones already in place.
  In particular, the *change load balancer weight* takes care of distributing the workload between the different instances of the same service.

  Managin and managed were deployed normally. During the simulation, however, the ART of the ordering-service is raised twice, after 90 seconds and after 180 seconds from the start of the simulation, via the method [getSleepForOrderingServiceInstances](./managed-system/rest-client/src/main/java/sefa/restclient/domain/PerformanceFakerService.java) settings its value in the [application.properties](./managed-system/rest-client/src/main/resources/application.properties) file.
  ```
  FAKE_SLOW_ORDERING=Y
  
  FAKE_SLOW_ORDERING_1_SLEEP=1000
  FAKE_SLOW_ORDERING_2_SLEEP=600
  
  FAKE_SLOW_ORDERING_1_START=90
  FAKE_SLOW_ORDERING_1_DURATION=60
  FAKE_SLOW_ORDERING_2_START=180
  FAKE_SLOW_ORDERING_2_DURATION=60
  ```

  By doing so, the managing will start other ordering services in addition to the existing ones, and the chance load weight balancing function will take care of splitting the work and, very importantly, save any new weight changes in the GitHub repository used by the configuration server.

  Configurations are saved each time they are changed, so that each time the managing is restarted, the changes are retrieved during the knowledge start-up. It is therefore important to clean up the configuration repo each time you want to re-deploy a clean managing system.

  In this scenario, at a certain point of the simulation, after starting two new order services, we will find weights equally distributed and thus saved in the configuration server:
  ```
  loadbalancing.ordering-service.sefa-ordering-service-34489_34489.weight=0.3333333333333333
  loadbalancing.ordering-service.sefa-ordering-service_58086.weight=0.3333333333333333
  loadbalancing.ordering-service.sefa-ordering-service-46293_46293.weight=0.3333333333333333
  ```

* ## Scenario 4 - *handleShutdownInstanceOption*

  In this last scenario we see the last of the adaptation options that RAMSES is able to implement, namely *handleShutdownInstanceOption*.

  We create a simulation where after 90 seconds from the start we deliberately slow down the ordering-service for one minute (after which it will be reset to normal values), this will cause the managing to start a new instance to help our slowed down service. Always through the [application.properties](./managed-system/rest-client/src/main/resources/application.properties).

  ```
  FAKE_SLOW_ORDERING=Y
  FAKE_SLOW_ORDERING_1_SLEEP=1000
  FAKE_SLOW_ORDERING_1_START=90
  FAKE_SLOW_ORDERING_1_DURATION=60
  ```

  After this, the work to be done will be handled by two instances that will divide the workload in half by setting weights (as seen in the previous scenario) resulting in the configuration repo as follows:
  
  ```
  loadbalancing.ordering-service.sefa-ordering-service-42315_42315.weight=0.5
  loadbalancing.ordering-service.sefa-ordering-service_58086.weight=0.5
  ```

   At this point, after 260 seconds from the beginning, we cause one of the two instances to decrease its availability of the 28%, so that the average availability of the entire ordering-service straddles the threshold.
  In this way, the managing will have to take note of the unfollowed threshold and act accordingly, in other words add another instances or change the load balancing weights again. Another times through the same properties file:

  ```
  FAKE_EXCEPTION_ORDERING=Y
  FAKE_EXCEPTION_VALUE_1=0.28
  FAKE_EXCEPTION_START_1=260
  ```

  Finally, the managing will realise that the threshold has not been met and will change the weights of the two instances, reducing the workload of the least available instance to zero and thus shutting it down definitively.

  ```
  Executing adaptation option: Goal: Availability - Change LBW. Service: ORDERING-SERVICE
  	New weights are:
  	{ordering-service@sefa-ordering-service-42315:42315=1.0}.
  SHUTDOWN INSTANCE REQUEST: serviceId= ORDERING-SERVICE, instanceToShutdownId=ordering-service@sefa-ordering-service:58086
  ```

  And consequently updating the configuration repo in this way:

   ```
   loadbalancing.ordering-service.sefa-ordering-service-42315_42315.weight=1.0
   ```

  If the availability is very close or straddles the threshold, it is more likely that the weights will be changed, otherwise, if there is a large gap between the actual availability and the threshold, a new instance will be added to help the two already present.

  In this same simulation, it happened to see both a change of weights as desired and the addition of a new instance. This is due to the random part of the simulation, fake purchases and various mathematical calculations which even in a real scenario cannot be controlled.

# Troubleshooting and Known Issues

1) A known issue on macOS involves the Actuator component, that sometimes cannot directly contact the Docker interface to run or stop containers. This results in the Instances Manager container to fail its booting process. To solve this issue, install socat using [this guide](https://stackoverflow.com/questions/16808543/install-socat-on-mac) and run the command:
   ```
   $ socat -d TCP-LISTEN:2375,range=0.0.0.0/0,reuseaddr,fork UNIX:/var/run/docker.sock
   ```
   
2) Clean the *application.properties* file in the configuration server repo on GitHub after each adaptation.
3) Sometimes the Knowledge container is not able to find correctly all the service. Just try to restart the knowledge container, and only after its start-up, relaunch all remaining managing containers.
   This is a problem of the Discovery Client not fast enough to register or unregister services.
