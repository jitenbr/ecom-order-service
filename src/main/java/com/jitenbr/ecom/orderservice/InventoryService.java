package com.jitenbr.ecom.orderservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    @Autowired
    @Qualifier("inventory-service-reserve-stock")
    WebClient webClient;

    @Autowired
    EcomOrderRepository orderRepository;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Autowired
    Producer producer;

    public void reserveProductStock(EcomOrder order, String token) {

        Map<String, Integer> products = order.getProducts();
        log.info("Checking stock availability within the InventoryService: {}", products);
        log.info("Sending request to inventory service to check stock availability: {}", products);

        for (Map.Entry<String, Integer> entry : products.entrySet()) {
            StockRequest stockRequest = new StockRequest();
            stockRequest.setProductid(entry.getKey());
            stockRequest.setQuantity(entry.getValue());

            AtomicInteger retryCounter = new AtomicInteger(0);

            // .bodyValue(BodyInserters.fromValue(stockRequest))
            Mono<String> inventoryServiceResponse = webClient.post()
                    .header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(stockRequest)
                    //.bodyValue(BodyInserters.fromValue(stockRequest))
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(Retry.fixedDelay(5, Duration.ofSeconds(10))
                    .doBeforeRetry(retrySignal -> {retryCounter.incrementAndGet(); log.info("Retrying..."+retryCounter);})
                    .filter(throwable -> throwable instanceof RuntimeException));

            log.info("inventory reservation  request sent to Inventory service for product: {}", stockRequest.getProductid());

            String responseKey = order.getOrderid()   ; //+(new Random().nextInt(1000));
            log.info("Response Key generated: {}", responseKey);
            redisTemplate.opsForValue().set(responseKey,"Checking Inventory for orderid:"+order.getOrderid()); // this is the response from the payment service
            log.info("Response Key set in the cache: {}", responseKey);


            inventoryServiceResponse.subscribe(
                    (response) ->
                    {
                        log.info(response+" from the inventory service");
                        order.setStatus("RESERVING INVENTORY");
                        orderRepository.save(order);
                        try
                        {
                            producer.publishOrderDatum(order.getOrderid(),
                                    "UPDATE",
                                    "Order Status updated to RESERVING INVENTORY for product id" + entry.getKey() + " response: " + response,
                                    order.getStatus(),
                                    order.getPayment_id());
                        }
                        catch (JsonProcessingException e)
                        {
                            throw new RuntimeException(e);
                        }
                        log.info("Updated status of the Order to RESERVING INVENTORY successfully");
                        redisTemplate.opsForValue().set(responseKey,"Inventory reserved successfully "+response);
                    },
                    error ->
                    {
                        log.info("error reserving product processing the response "+error.getMessage());

                        log.info("Updating status of the Order to FAILED");

                        order.setStatus("INVENTORY FAILED");
                        orderRepository.save(order);
                        try
                        {
                            producer.publishOrderDatum(order.getOrderid(),
                                    "UPDATE",
                                    "Order Status updated to INVENTORY FAILED for product id" + entry.getKey() + " response: " + error.getMessage(),
                                    order.getStatus(),
                                    order.getPayment_id());
                        }
                        catch (JsonProcessingException e)
                        {
                            throw new RuntimeException(e);
                        }
                        log.info("Updated status of the Order to FAILED successfully");

                        redisTemplate.opsForValue().set(responseKey," FAILED error "+error.getMessage());
                    });

        }

    }

}

@Getter
@Setter
class StockRequest {

    private String productid;

    private Integer quantity;

}