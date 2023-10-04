package it.polimi.sefa.paymentproxy2service.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import it.polimi.sefa.paymentproxy2service.externalinterface.PaymentRequest;

@Service
public class PaymentProxyService {
	@Value("${payment.service.uri}")
	private String paymentServiceUri;

	public boolean processPayment(
		String cardNumber,
		int expMonth,
		int expYear,
		String cvv,
		double amount
	) {
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response = restTemplate.postForEntity(paymentServiceUri, new PaymentRequest(cardNumber, expMonth, expYear, cvv, amount), String.class);
		return response.getStatusCode().is2xxSuccessful();
	}
	
}

