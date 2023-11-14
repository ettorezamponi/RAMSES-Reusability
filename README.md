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

* ## Scenario 2 - *handleChangeImplementationOption*

  This represents the most complete scenario in that the managing recognises a better implementation, stops the old container and starts the new one with higher restrictions.
  
  This was done modifiyng [qos_specification.json](./managing-system/knowledge/architecture_sla/sefa/qos_specification.json) and running a simple purchase simulation for the short duration of 2 minutes:
  
  ```
  "name" : "average_response_time",
  "weight" : 0.5,
  "max_threshold": 150
  ```

  An unfeasible test scenario was implemented by setting a threshold for the average response time of 150 compared to the classical 500 for the delivery.
None of the three delivery services is able to meet this, but it was done on purpose to see the reaction of RAMSES. 

  As soon as the Analyse Window Size is full, a change implementation option will take place. After that the QoS history will be deleted and the system will start collecting data again, until it will realisee that even the new instance cannot satisfy the unreachable threshold and will allocate a new additional service (this is the reason of the short simulation provided).


* Scenario 3
  
  *handleChangeLBWeightsOption*
  ...
* Scenario 4
  
  *handleShutdownInstanceOption*
  ...
  
