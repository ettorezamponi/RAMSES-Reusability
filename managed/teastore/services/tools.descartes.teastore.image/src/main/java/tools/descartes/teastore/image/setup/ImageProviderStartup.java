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
package tools.descartes.teastore.image.setup;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import io.opentracing.util.GlobalTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.descartes.teastore.registryclient.RegistryClient;
import tools.descartes.teastore.registryclient.Service;
import tools.descartes.teastore.registryclient.StartupCallback;
import tools.descartes.teastore.registryclient.loadbalancers.ServiceLoadBalancer;
import tools.descartes.teastore.registryclient.tracing.Tracing;
import tools.ezamponi.MyEurekaClientHelper;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Application Lifecycle Listener implementation class Registry Client Startup.
 *
 * @author Simon Eismann
 *
 */
@WebListener
public class ImageProviderStartup implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(ImageProviderStartup.class);
    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();


    /**
   * Empty constructor.
   */
  public ImageProviderStartup() {

  }

  /**
   * @see ServletContextListener#contextDestroyed(ServletContextEvent)
   * @param event
   *          The servlet context event at destruction.
   */
  public void contextDestroyed(ServletContextEvent event) {
    RegistryClient.getClient().unregister(event.getServletContext().getContextPath());
    SetupController.SETUP.teardown();

    MyEurekaClientHelper.deRegister();
    LOG.info("Shutdown image eureka client");
  }

  /**
   * @see ServletContextListener#contextInitialized(ServletContextEvent)
   * @param event
   *          The servlet context event at initialization.
   */
  public void contextInitialized(ServletContextEvent event) {
    GlobalTracer.register(Tracing.init(Service.IMAGE.getServiceName()));
    ServiceLoadBalancer.preInitializeServiceLoadBalancers(Service.PERSISTENCE);
    RegistryClient.getClient().runAfterServiceIsAvailable(Service.PERSISTENCE,
        new StartupCallback() {
          @Override
          public void callback() {
            SetupController.SETUP.startup();
            RegistryClient.getClient().register(event.getServletContext().getContextPath());
          }
        }, Service.IMAGE);

      LOG.info("Image online");
      LOG.info("-------------------------------------------------------------------------------------------------");

      executorService.schedule(() -> {
          MyEurekaClientHelper.register();
      }, 40, TimeUnit.SECONDS);

      executorService.shutdown();
  }
}