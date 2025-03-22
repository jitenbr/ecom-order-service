package com.jitenbr.ecom.orderservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class OrderHelperService {

    private static final Logger log = LoggerFactory.getLogger(MainRestController.class);

    @Autowired
    ProductService productService;

    public Integer calculateOrder(EcomOrder order, String token)
    {

        log.info("Calculating total amount for order: {}", order);
        Integer total = 0;

        for(Map.Entry<String, Integer> entry : order.getProducts().entrySet())
        {

            log.info("Processing dish: {}", entry.getKey());
            Product product = productService.getProduct(entry.getKey(),token);
            log.info("Found Product item: {}", product);

            if(product == null)
            {
                return -1;
            }

            total += product.getPrice() * entry.getValue(); // multiplying price with quantity
            log.info("Total amount so far: {}", total);
        }

        log.info("Total amount calculated: {}", total);
        return total;
    }


}
