package sefa.orderingservice.externalInterfaces;

import sefa.orderingservice.config.LoadBalancerConfig;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import sefa.deliveryproxyservice.restapi.DeliverOrderRequest;
import sefa.deliveryproxyservice.restapi.DeliverOrderResponse;

@FeignClient(name = "DELIVERY-PROXY-SERVICE")
@LoadBalancerClient(name = "DELIVERY-PROXY-SERVICE", configuration = LoadBalancerConfig.class)
public interface DeliveryProxyClient {
    @PostMapping("/rest/deliverOrder")
    DeliverOrderResponse deliverOrder(@RequestBody DeliverOrderRequest request);
}
