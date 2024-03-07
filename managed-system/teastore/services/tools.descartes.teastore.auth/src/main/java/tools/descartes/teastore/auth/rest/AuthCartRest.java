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

package tools.descartes.teastore.auth.rest;

import io.micrometer.core.instrument.Timer;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import jakarta.ws.rs.core.UriInfo;
import tools.descartes.teastore.auth.security.ShaSecurityProvider;
import tools.descartes.teastore.auth.startup.AuthStartup;
import tools.descartes.teastore.entities.OrderItem;
import tools.descartes.teastore.entities.Product;
import tools.descartes.teastore.entities.message.SessionBlob;
import tools.descartes.teastore.registryclient.Service;
import tools.descartes.teastore.registryclient.rest.LoadBalancedCRUDOperations;
import tools.descartes.teastore.registryclient.util.NotFoundException;
import tools.descartes.teastore.registryclient.util.TimeoutException;
import tools.ezamponi.MetricsExporter;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Rest endpoint for the store cart.
 * 
 * @author Simon
 */
@Path("cart")
@Produces({ "application/json" })
@Consumes({ "application/json" })
public class AuthCartRest {

  /**
   * Adds product to cart. If the product is already in the cart the quantity is
   * increased.
   * 
   * @param blob
   *          Sessionblob
   * @param pid
   *          productid
   * @return Response containing session blob with updated cart
   */
  @POST
  @Path("add/{pid}")
  public Response addProductToCart(SessionBlob blob, @PathParam("pid") final Long pid) {
    long startTime = System.currentTimeMillis();

    Product product;
    try {
      product = LoadBalancedCRUDOperations.getEntity(Service.PERSISTENCE, "products", Product.class,
          pid);
    } catch (TimeoutException e) {
      // Histogram metrics
      long duration = (long) (System.currentTimeMillis()-startTime * (1 + (AuthStartup.delay/100.0)));;
      Timer addTimer = MetricsExporter.createTimerErrorMetric("POST", "/addToCart");
      addTimer.record(duration, TimeUnit.MILLISECONDS);
      return Response.status(408).build();
    } catch (NotFoundException e) {
      // Histogram metrics
      long duration = (long) (System.currentTimeMillis()-startTime * (1 + (AuthStartup.delay/100.0)));;
      Timer addTimer = MetricsExporter.createTimerErrorMetric("POST", "/addToCart");
      addTimer.record(duration, TimeUnit.MILLISECONDS);
      return Response.status(404).build();
    }

    for (OrderItem orderItem : blob.getOrderItems()) {
      if (orderItem.getProductId() == pid) {
        orderItem.setQuantity(orderItem.getQuantity() + 1);
        blob = new ShaSecurityProvider().secure(blob);
        return Response.status(Response.Status.OK).entity(blob).build();
      }
    }
    OrderItem item = new OrderItem();
    item.setProductId(pid);
    item.setQuantity(1);
    item.setUnitPriceInCents(product.getListPriceInCents());
    blob.getOrderItems().add(item);
    blob = new ShaSecurityProvider().secure(blob);
    // Histogram metrics
    long duration = (long) (System.currentTimeMillis()-startTime * (1 + (AuthStartup.delay/100.0)));;
    Timer addTimer = MetricsExporter.createTimerMetric("POST", "/addToCart", AuthStartup.httpSuccessProbability, new Random().nextDouble());
    addTimer.record(duration,TimeUnit.MILLISECONDS);
    //System.out.println("TIMER CREATO con tempo: "+duration);
    return Response.status(Response.Status.OK).entity(blob).build();
  }

  /**
   * Remove product from cart.
   * 
   * @param blob
   *          Sessionblob
   * @param pid
   *          product id
   * @return Response containing Sessionblob with updated cart
   */
  @POST
  @Path("remove/{pid}")
  public Response removeProductFromCart(SessionBlob blob, @PathParam("pid") final Long pid) {
    long startTime = System.currentTimeMillis();
    OrderItem toRemove = null;
    for (OrderItem item : blob.getOrderItems()) {
      if (item.getProductId() == pid) {
        toRemove = item;
      }
    }
    if (toRemove != null) {
      blob.getOrderItems().remove(toRemove);
      blob = new ShaSecurityProvider().secure(blob);
      // Histogram metrics
      long duration = (long) (System.currentTimeMillis()-startTime * (1 + (AuthStartup.delay/100.0)));;
      Timer addTimer = MetricsExporter.createTimerMetric("POST", "/removeProduct", AuthStartup.httpSuccessProbability, new Random().nextDouble());
      addTimer.record(duration,TimeUnit.MILLISECONDS);

      return Response.status(Response.Status.OK).entity(blob).build();
    } else {
      // Histogram metrics
      long duration = (long) (System.currentTimeMillis()-startTime * (1 + (AuthStartup.delay/100.0)));;
      Timer addTimer = MetricsExporter.createTimerErrorMetric("POST", "/removeProduct");
      addTimer.record(duration, TimeUnit.MILLISECONDS);
      return Response.status(Response.Status.NOT_FOUND).build();
    }
  }

  /**
   * Updates quantity of product in cart.
   * 
   * @param blob
   *          Sessionblob
   * @param pid
   *          Productid
   * @param quantity
   *          New quantity
   * @return Response containing Sessionblob with updated cart
   */
  @PUT
  @Path("{pid}")
  public Response updateQuantity(SessionBlob blob, @PathParam("pid") final Long pid,
      @QueryParam("quantity") int quantity) {
    long startTime = System.currentTimeMillis();
    for (OrderItem item : blob.getOrderItems()) {
      if (item.getProductId() == pid) {
        item.setQuantity(quantity);
        blob = new ShaSecurityProvider().secure(blob);
        // Histogram metrics
        long duration = (long) (System.currentTimeMillis()-startTime * (1 + (AuthStartup.delay/100.0)));;
        Timer addTimer = MetricsExporter.createTimerMetric("PUT", "/updateQuantity", AuthStartup.httpSuccessProbability, new Random().nextDouble());
        addTimer.record(duration,TimeUnit.MILLISECONDS);

        return Response.status(Response.Status.OK).entity(blob).build();
      }
    }
    // Histogram metrics
    long duration = (long) (System.currentTimeMillis()-startTime * (1 + (AuthStartup.delay/100.0)));;
    Timer addTimer = MetricsExporter.createTimerErrorMetric("PUT", "/updateQuantity");
    addTimer.record(duration, TimeUnit.MILLISECONDS);
    return Response.status(Response.Status.NOT_FOUND).build();
  }

  @GET
  @Path("/prometheus") // http://localhost:8080/tools.descartes.teastore.auth/rest/cart/prometheus
  @Produces(MediaType.TEXT_PLAIN)
  public Response getMetrics(@Context UriInfo uriInfo) {
    String metrics = MetricsExporter.getMetrics(uriInfo);
    return Response.ok(metrics).build();
  }

}
