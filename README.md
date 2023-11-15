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

  Finally, it will inject a feasible threshold to return the situation to normal; otherwise RAMSES will continue to allocate container to respect the unfeasible threshold.
  

* Scenario 3
  
  *handleChangeLBWeightsOption*
  ...
* Scenario 4
  
  *handleShutdownInstanceOption*
  ...
  
