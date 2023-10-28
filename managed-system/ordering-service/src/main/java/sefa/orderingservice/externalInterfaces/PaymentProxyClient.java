package sefa.orderingservice.externalInterfaces;

import sefa.orderingservice.config.LoadBalancerConfig;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import sefa.paymentproxyservice.restapi.ProcessPaymentRequest;
import sefa.paymentproxyservice.restapi.ProcessPaymentResponse;

@FeignClient(name = "PAYMENT-PROXY-SERVICE")
@LoadBalancerClient(name = "PAYMENT-PROXY-SERVICE", configuration = LoadBalancerConfig.class)
public interface PaymentProxyClient {
    @PostMapping("/rest/processPayment")
    ProcessPaymentResponse processPayment(@RequestBody ProcessPaymentRequest request);
}
