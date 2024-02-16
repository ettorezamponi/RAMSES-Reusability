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
package tools.descartes.teastore.persistence.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Timer;
import jakarta.ws.rs.*;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import tools.descartes.teastore.persistence.domain.OrderRepository;
import tools.descartes.teastore.persistence.repository.DataGenerator;
import tools.descartes.teastore.registryclient.util.AbstractCRUDEndpoint;
import tools.descartes.teastore.entities.Order;
import tools.ezamponi.MetricsExporter;
import tools.ezamponi.util.UtilMethods;

/**
 * Persistence endpoint for CRUD operations on orders.
 * @author Joakim von Kistowski
 *
 */
@Path("orders")
public class OrderEndpoint extends AbstractCRUDEndpoint<Order> {
	// [0.0, 1.0]
	double httpSuccessProbability = 1;
	// percentage of slowing http request, [0, 100]
	long delay = 0;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected long createEntity(final Order order) {
		if (DataGenerator.GENERATOR.isMaintenanceMode()) {
			return -1L;
		}
		return OrderRepository.REPOSITORY.createEntity(order);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Order findEntityById(final long id) {
		Order order = OrderRepository.REPOSITORY.getEntity(id);
		if (order == null) {
			return null;
		}
		return new Order(order);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<Order> listAllEntities(final int startIndex, final int maxResultCount) {
		List<Order> order = new ArrayList<Order>();
		for (Order o : OrderRepository.REPOSITORY.getAllEntities(startIndex, maxResultCount)) {
			order.add(new Order(o));
		}
		return order;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean updateEntity(long id, Order order) {
		return OrderRepository.REPOSITORY.updateEntity(id, order);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean deleteEntity(long id) {
		if (DataGenerator.GENERATOR.isMaintenanceMode()) {
			return false;
		}
		return OrderRepository.REPOSITORY.removeEntity(id);
	}
	
	/**
	 * Returns all order items with the given product Id (all order items for that product).
	 * @param userId The id of the product.
	 * @param startPosition The index (NOT ID) of the first order item with the product to return.
	 * @param maxResult The max number of order items to return.
	 * @return list of order items with the product.
	 */
	@GET
	@Path("user/{user:[0-9][0-9]*}")
	public List<Order> listAllForUser(@PathParam("user") final Long userId,
			@QueryParam("start") final Integer startPosition,
			@QueryParam("max") final Integer maxResult) {

		long startTime = System.currentTimeMillis();

		if (userId == null) {
			long duration = (long) (UtilMethods.randomNumber(0.015,0.700) * (1 + (delay/100.0)));
			Timer addTimer = MetricsExporter.createTimerMetric("GET", "/listAllForUser", httpSuccessProbability, new Random().nextDouble());
			addTimer.record(duration, TimeUnit.MILLISECONDS);

			return listAll(startPosition, maxResult);
		}
		List<Order> orders = new ArrayList<Order>();
		for (Order o : OrderRepository.REPOSITORY.getAllEntitiesWithUser(userId,
				parseIntQueryParam(startPosition), parseIntQueryParam(maxResult))) {
			orders.add(new Order(o));
		}
		// Histogram metrics
		long duration = (long) (System.currentTimeMillis()-startTime * (1 + (delay/100.0)));
		Timer addTimer = MetricsExporter.createTimerMetric("GET", "/listAllForUser", httpSuccessProbability, new Random().nextDouble());
		addTimer.record(duration, TimeUnit.MILLISECONDS);
		return orders;
	}

	@GET
	@Path("/prometheus") // http://localhost:8080/tools.descartes.teastore.persistence/rest/orders/prometheus
	@Produces(MediaType.TEXT_PLAIN)
	public Response getMetrics(@Context UriInfo uriInfo) {
		String metrics = MetricsExporter.getMetrics(uriInfo);
		return Response.ok(metrics).build();
	}

}
