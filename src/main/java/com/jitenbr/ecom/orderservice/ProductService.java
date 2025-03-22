package com.jitenbr.ecom.orderservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    @Qualifier("product-service")
    WebClient webClient;


    Product getProduct(String productid, String token) {
        log.info("Getting product within the ProductService: {}", productid);
        log.info("Sending request to product service to get product: {}", productid);
        Product response = webClient.get().uri("/get/product/" + productid)
                .header("Authorization", token)
                .retrieve()
                .bodyToMono(Product.class).block(); // Current Thread will pause till the final response comes back
        log.info("Response from product service: {}", response);
        return response;
    }



}
