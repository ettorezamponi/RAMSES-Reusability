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

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Timer;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;
import tools.ezamponi.MetricsExporter;

/**
 * Rest endpoint for the registry.
 * @author Simon
 */
@Path("services")
@Produces({ "application/json" })
public class RegistryREST {

	/**
	 * Register a service at a location.
	 * @param name Service name
	 * @param location service location
	 * @return boolean success indicator
	 */
	@PUT
	@Path("{name}/{location}")
	public Response register(@PathParam("name") final String name, @PathParam("location") final String location) {
		long startTime = System.currentTimeMillis();
		boolean success = Registry.getRegistryInstance().register(name, location);
		if (success) {
			// Histogram metrics
			long duration = System.currentTimeMillis()-startTime;
			Timer addTimer = MetricsExporter.createTimerMetric("PUT", "/registerService");
			addTimer.record(duration, TimeUnit.MILLISECONDS);

			return Response.status(Status.CREATED).build();
		}
		// Histogram metrics
		long duration = System.currentTimeMillis()-startTime;
		Timer addTimer = MetricsExporter.createTimerMetric("PUT", "/registerService");
		addTimer.record(duration, TimeUnit.MILLISECONDS);

		return Response.ok().build();
	}

	/**
	 * Unregister a service at a location.
	 * @param name Service name
	 * @param location service location
	 * @return boolean success indicator
	 */
	@DELETE
	@Path("{name}/{location}")
	public Response unregister(@PathParam("name") final String name, @PathParam("location") final String location) {
		long startTime = System.currentTimeMillis();
		boolean success = Registry.getRegistryInstance().unregister(name, location);
		if (success) {
			// Histogram metrics
			long duration = System.currentTimeMillis()-startTime;
			Timer addTimer = MetricsExporter.createTimerMetric("PUT", "/unregisterService");
			addTimer.record(duration, TimeUnit.MILLISECONDS);

			return Response.status(Response.Status.OK).build();
		}
		// Histogram metrics
		long duration = System.currentTimeMillis()-startTime;
		Timer addTimer = MetricsExporter.createTimerMetric("PUT", "/unregisterService");
		addTimer.record(duration, TimeUnit.MILLISECONDS);

		return Response.status(Response.Status.NOT_FOUND).build();
	}

	/**
	 * Get list of all instances of a service.
	 * @param name service name
	 * @return list of all instance
	 */
	@GET
	@Path("{name}")
	public Response getInstances(@PathParam("name") final String name) {
		long startTime = System.currentTimeMillis();
		List<String> locations = Registry.getRegistryInstance().getLocations(name);
		// Histogram metrics
		long duration = System.currentTimeMillis()-startTime;
		Timer addTimer = MetricsExporter.createTimerMetric("GET", "/getInstanceService");
		addTimer.record(duration, TimeUnit.MILLISECONDS);

		return Response.status(Response.Status.OK).entity(locations).build();
	}

	@GET
	@Path("/prometheus") // http://localhost:8080/tools.descartes.teastore.registry/rest/services/prometheus
	@Produces(MediaType.TEXT_PLAIN)
	public Response getMetrics(@Context UriInfo uriInfo) {
		String metrics = MetricsExporter.getMetrics(uriInfo);
		return Response.ok(metrics).build();
	}

}
