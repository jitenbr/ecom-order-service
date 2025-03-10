package com.secor.orderservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Random;

@Service
public class PaymentService
{
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Autowired
    @Qualifier("payment-service-create-payment")
    WebClient webClient;

            public String createPayment(PaymentRequest paymentRequest, String token)
            {
                log.info("Received request to create payment: {}", paymentRequest);


                Mono<String> paymentServiceResponse = webClient.post()
                        .header("Authorization", token)
                        .body(BodyInserters.fromValue(paymentRequest))
                        .retrieve()
                        .bodyToMono(String.class);

                log.info("Payment Request sent to the payment service");

                String responseKey = paymentRequest.getOrder_id()+(new Random().nextInt(1000)); // this is the key that we will return from this method

                log.info("Response Key generated: {}", responseKey);
                // eventual response from the payment service will  come a bit later but we will  proceed with our thread execution

                // here goes the handler for the eventual response and one of the primary responsibilities of this handler is to put the
                // response in the cache so that any one else with the right cache key can retrieve the response from the cache and we then return the key from this method

                // SECOND PART OF ASYNC REQUEST - TO SET UP THE HANDLER FOR THE EVENTUAL RESPONSE

                redisTemplate.opsForValue().set(responseKey,"stage1 order-processing");
                log.info("Response Key set in the cache: {}", responseKey);


                paymentServiceResponse.subscribe(
                        (response) ->
                        {
                            log.info(response+" from the payment service");
                            // MENU CREATION LOGIC TO BE IMPLEMENTED HERE
                            // AND PUT THE RESPONSE IN REDIS
                            redisTemplate.opsForValue().set(responseKey,"payment_id "+response);
                        },
                        error ->
                        {
                            log.info("error processing the response "+error.getMessage());
                            redisTemplate.opsForValue().set(responseKey,"error "+error.getMessage());
                        });


                //redisTemplate.opsForValue().set(responseKey, response.block()); // this is the response from the payment service


                log.info("Returning the response key: {}", responseKey);
                return responseKey;
            }

}
