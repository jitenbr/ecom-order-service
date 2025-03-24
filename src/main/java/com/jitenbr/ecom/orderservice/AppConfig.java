package com.jitenbr.ecom.orderservice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {


    @Bean(name = "auth-service")
    public WebClient webClientAuthService(WebClient.Builder webClientBuilder)
    {
        return webClientBuilder
                .baseUrl("http://localhost:8082/api/v1")
                .filter(new LoggingWebClientFilter())
                .build();
    }



    @Bean(name = "payment-service-create-payment")
    public WebClient webClientPymntService(WebClient.Builder webClientBuilder)
    {
        return webClientBuilder
                .baseUrl("http://localhost:8102/api/v1/create/payment")
                .filter(new LoggingWebClientFilter())
                .build();
    }

    @Bean(name = "inventory-service-reserve-stock")
    public WebClient webClientInventoryService(WebClient.Builder webClientBuilder)
    {
        return webClientBuilder
                .baseUrl("http://localhost:8103/api/v1/reserve/stock")
                .filter(new LoggingWebClientFilter())
                .build();
    }

    @Bean(name = "product-service")
    public WebClient webClientProductService(WebClient.Builder webClientBuilder)
    {
        return webClientBuilder
                .baseUrl("http://localhost:8087/api/v1")
                .filter(new LoggingWebClientFilter())
                .build();
    }

}
