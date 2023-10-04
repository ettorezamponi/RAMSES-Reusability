# SEFA - A SErvice-based eFood Application
MSc final thesis project by Vincenzo Riccio and Giancarlo Sorrentino.

## Third party services

### Payment Service
Three payment services are simulated. 

Given that `cvv` is the integer value of the CVV of a credit/debit card used to pay an order, they return `HTTP.200` with a text response after waiting `cvv/10` seconds.
If `cvv` is `0`, they return `HTTP.500` with a text response to simulate a server error.

They run in the port range `58090-58092` by default.

To ease the development, they are also hosted on Vercel, which can be reached at the following URLs:

|         Service                                                        |
|     :-------------:                                                    |
|    [Payment Service 1](https://payment-service-ramses.vercel.app/1)    |
|    [Payment Service 2](https://payment-service-ramses.vercel.app/2)    |
|    [Payment Service 3](https://payment-service-ramses.vercel.app/3)    |


### Delivery Service
Three delivery services are simulated. 

Given that `number` is the integer value of the street number of the delivery address of an order, they return `HTTP.200` with a text response after waiting `number/10` seconds.
If `number` is `0`, they return `HTTP.500` with a text response to simulate a server error.

They run in the port range `58095-58097` by default.

To ease the development, they are also hosted on Vercel, which can be reached at the following URLs:

|         Service                                                        |
|     :-------------:                                                    |
|    [Delivery Service 1](https://delivery-service-ramses.vercel.app/1)  |
|    [Delivery Service 2](https://delivery-service-ramses.vercel.app/2)  |
|    [Delivery Service 3](https://delivery-service-ramses.vercel.app/3)  |
