package sefa.deliveryproxy3service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class DeliveryProxyServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeliveryProxyServiceApplication.class, args);
	}

}

