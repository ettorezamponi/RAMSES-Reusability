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
package tools.descartes.teastore.recommender.rest;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Timer;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import jakarta.ws.rs.core.UriInfo;
import tools.descartes.teastore.recommender.algorithm.RecommenderSelector;
import tools.descartes.teastore.entities.OrderItem;
import tools.descartes.teastore.entities.Product;
import tools.descartes.teastore.entities.User;
import tools.ezamponi.MetricsExporter;

/**
 * Recommender REST endpoint.
 * 
 * @author Johannes Grohmann
 *
 */
@Path("recommend")
@Produces({ "application/json" })
@Consumes({ "application/json" })
public class RecommendEndpoint {
	double httpSuccessProbability = 1;

	/**
	 * Return a list of all {@link Product}s, that are recommended for the given
	 * {@link User} buying the given list of {@link OrderItem}s. <br>
	 * 
	 * The returning list does not contain any {@link Product} that is already part
	 * of the given list of {@link OrderItem}s. It might be empty, however.
	 * 
	 * @param currentItems
	 *            A list, containing all {@link OrderItem}s in the current cart.
	 *            Might be empty.
	 * @param uid
	 *            The id of the {@link User} to recommend for. May be null.
	 * @return List of {@link Long} objects, containing all {@link Product} IDs that
	 *         are recommended to add to the cart, or an INTERNALSERVERERROR, if the
	 *         recommendation failed.
	 */
	@POST
	public Response recommend(List<OrderItem> currentItems, @QueryParam("uid") final Long uid) {
		long startTime = System.currentTimeMillis();
		List<Long> recommended = RecommenderSelector.getInstance().recommendProducts(uid, currentItems);
		// Histogram metrics
		long duration = System.currentTimeMillis()-startTime;
		Timer addTimer = MetricsExporter.createTimerMetric("POST", "/recommend", httpSuccessProbability, new Random().nextDouble());
		addTimer.record(duration, TimeUnit.MILLISECONDS);

		return Response.ok().entity(recommended).build();
	}

	@GET
	@Path("/prometheus") // http://localhost:8080/tools.descartes.teastore.recommender/rest/recommend/prometheus
	@Produces(MediaType.TEXT_PLAIN)
	public Response getMetrics(@Context UriInfo uriInfo) {
		String metrics = MetricsExporter.getMetrics(uriInfo);
		return Response.ok(metrics).build();
	}
}
