/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tools.descartes.teastore.registry.rest;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.smattme.eureka.client.wrapper.EurekaClientService;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application Lifecycle Listener implementation class Registry Client Startup.
 *
 * @author Simon Eismann
 *
 */
@WebListener
public class RegistryStartup implements ServletContextListener {

  EurekaClientService eurekaClientService = new EurekaClientService();

  private static final Logger LOG = LoggerFactory.getLogger(RegistryStartup.class);
  /**
   * Also set this accordingly in RegistryClientStartup.
   */
  private static final int HEARTBEAT_INTERVAL_MS = 2500;

  private static ScheduledExecutorService heartbeatScheduler;


  /**
   * Empty constructor.
   */
  public RegistryStartup() {

  }

  /**
   * @see ServletContextListener#contextDestroyed(ServletContextEvent)
   * @param arg0
   *          The servlet context event at destruction.
   */
  public void contextDestroyed(ServletContextEvent arg0) {
    heartbeatScheduler.shutdownNow();
    eurekaClientService.deRegister();
    LOG.info("Shutdown registry and eureka client");
  }

  /**
   * @see ServletContextListener#contextInitialized(ServletContextEvent)
   * @param arg0
   *          The servlet context event at initialization.
   */
  public void contextInitialized(ServletContextEvent arg0) {
    heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
    heartbeatScheduler.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        Registry.getRegistryInstance().heartBeatCleanup();
      }
    }, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    LOG.info("Registry online");
    LOG.info("-------------------------------------------------------------------------------------------------");

    
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    Thread myThread = new Thread(() -> {
      try {
        // Codice del thread
        LOG.info("Thread started after 6 seconds");
        eurekaClientService.registerInstance();

      } catch (Exception e) {
        e.printStackTrace();
      }
    });

    myThread.start();
  }
}
