package it.polimi.sefa.orderingservice.exceptions;

public class PaymentDetailsNotValidException extends RuntimeException{
    public PaymentDetailsNotValidException(String message) {
        super(message);
    }
}
