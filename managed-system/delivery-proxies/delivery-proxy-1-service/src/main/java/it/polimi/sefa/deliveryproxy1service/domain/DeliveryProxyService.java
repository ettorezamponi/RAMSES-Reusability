package it.polimi.sefa.deliveryproxy1service.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import it.polimi.sefa.deliveryproxy1service.externalinterface.DeliverRequest;

import java.util.Date;

@Service
public class DeliveryProxyService {
	@Value("${delivery.service.uri}")
	private String deliveryServiceUri;

 	public boolean deliverOrder(
		 String address,
		 String city,
		 int number,
		 String zipcode,
		 String telephoneNumber,
		 Date scheduledTime
	) {
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response = restTemplate.postForEntity(deliveryServiceUri, new DeliverRequest(address, city, number, zipcode, telephoneNumber, scheduledTime), String.class);
		return response.getStatusCode().is2xxSuccessful();
	}
	
}

