package ramses.loadbalancer.algorithms;

import ramses.loadbalancer.BaseLoadBalancer;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


public class RoundRobinLoadBalancer extends BaseLoadBalancer {

    protected final AtomicInteger position;


    public RoundRobinLoadBalancer(ServiceInstanceListSupplier serviceInstanceListSupplierProvider) {
        this(serviceInstanceListSupplierProvider, new Random().nextInt(500));
    }

    public RoundRobinLoadBalancer(ServiceInstanceListSupplier serviceInstanceListSupplierProvider, int seedPosition) {
        super(serviceInstanceListSupplierProvider);
        this.position = new AtomicInteger(seedPosition);
    }

    @Override
    protected Response<ServiceInstance> processInstanceResponse(List<ServiceInstance> serviceInstances) {
        Response<ServiceInstance> serviceInstanceResponse;
        if (serviceInstances.isEmpty()) {
            if (log.isWarnEnabled())
                log.warn("No servers available for service: " + getServiceId());
            serviceInstanceResponse = new EmptyResponse();
        } else { // Ignore the sign bit, this allows pos to loop sequentially from 0 to Integer.MAX_VALUE
            int pos = this.position.incrementAndGet() & Integer.MAX_VALUE;
            ServiceInstance instance = serviceInstances.get(pos % serviceInstances.size());
            serviceInstanceResponse = new DefaultResponse(instance);
            if (log.isDebugEnabled()) {
                log.debug("RoundRobinLoadBalancer: selected instance "+instance.getInstanceId());
            }
        }
        return serviceInstanceResponse;
    }

}
