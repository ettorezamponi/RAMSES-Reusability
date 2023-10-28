package sefa.orderingservice.restapi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemElement {
    private String id;
    private String name;
    private double price;
    private int quantity;

    public double getTotalPrice() {
        return price * quantity;
    }
}
