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

package tools.descartes.teastore.webui.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import tools.ezamponi.MetricsExporter;

import java.io.IOException;

/**
 * Rest endpoint for the store cart.
 *
 * @author Simon
 */
@Path("ready")
@Produces({ "application/json" })
public class ReadyRest {


  /**
   * This methods checks, if the service is ready.
   *
   * @return True, if recommender is ready; false, if not.
   */
  @GET
  @Path("isready")
  public Response isReady() {
    return Response.ok(true).build();
  }

  @GET
  @Path("/prometheus") // http://localhost:8080/tools.descartes.teastore.webui/rest/ready/prometheus
  @Produces(MediaType.TEXT_PLAIN)
  public Response getMetrics(@Context UriInfo uriInfo) {
    String metrics = MetricsExporter.getMetrics(uriInfo);
    return Response.ok(metrics).build();
  }

}
