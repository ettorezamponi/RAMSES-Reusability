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
package tools.descartes.teastore.webui.servlet;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Timer;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import tools.descartes.teastore.registryclient.Service;
import tools.descartes.teastore.registryclient.loadbalancers.LoadBalancerTimeoutException;
import tools.descartes.teastore.registryclient.rest.LoadBalancedCRUDOperations;
import tools.descartes.teastore.registryclient.rest.LoadBalancedImageOperations;
import tools.descartes.teastore.registryclient.rest.LoadBalancedStoreOperations;
import tools.descartes.teastore.webui.servlet.elhelper.ELHelperUtils;
import tools.descartes.teastore.entities.Category;
import tools.descartes.teastore.entities.ImageSizePreset;
import tools.descartes.teastore.entities.Order;
import tools.descartes.teastore.entities.User;
import tools.ezamponi.MetricsExporter;

/**
 * Servlet implementation for the web view of "Profile".
 * 
 * @author Andre Bauer
 */
@WebServlet("/profile")
public class ProfileServlet extends AbstractUIServlet {

  private static final long serialVersionUID = 1L;
  // [0.0, 1.0]
  double httpSuccessProbability = 1;
  // percentage of slowing http request, [0, 100]
  long delay = 0;

  /**
   * @see HttpServlet#HttpServlet()
   */
  public ProfileServlet() {
    super();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void handleGETRequest(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException, LoadBalancerTimeoutException {
    long startTime = System.currentTimeMillis();
    checkforCookie(request, response);
    if (!LoadBalancedStoreOperations.isLoggedIn(getSessionBlob(request))) {
      redirect("/", response);
    } else {

      request.setAttribute("storeIcon",
          LoadBalancedImageOperations.getWebImage("icon", ImageSizePreset.ICON.getSize()));
      request.setAttribute("CategoryList", LoadBalancedCRUDOperations
          .getEntities(Service.PERSISTENCE, "categories", Category.class, -1, -1));
      request.setAttribute("title", "TeaStore Home");
      request.setAttribute("User", LoadBalancedCRUDOperations.getEntity(Service.PERSISTENCE,
          "users", User.class, getSessionBlob(request).getUID()));
      request.setAttribute("Orders", LoadBalancedCRUDOperations.getEntities(Service.PERSISTENCE,
          "orders", Order.class, "user", getSessionBlob(request).getUID(), -1, -1));
      request.setAttribute("login",
          LoadBalancedStoreOperations.isLoggedIn(getSessionBlob(request)));
      request.setAttribute("helper", ELHelperUtils.UTILS);

      request.getRequestDispatcher("WEB-INF/pages/profile.jsp").forward(request, response);
    }
    // Histogram metrics
    long duration = (long) ((System.currentTimeMillis()-startTime) * (1 + (delay/100.0)));
    Timer addTimer = MetricsExporter.createTimerMetric("GET", "/profile", httpSuccessProbability, new Random().nextDouble());
    addTimer.record(duration, TimeUnit.MILLISECONDS);

  }

}