package com.jitenbr.ecom.orderservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("api/v1")
public class MainRestController {

    private static final Logger log = LoggerFactory.getLogger(MainRestController.class);

    @Autowired
    EcomOrderRepository ecomOrderRepository;

    @Autowired
    AuthService authService;

    @Autowired
    PaymentService paymentService;

    @Autowired
    InventoryService inventoryService;

    @Autowired
    OrderHelperService orderHelperService;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Autowired
    Producer producer;

    @GetMapping("get/order/{orderid}")
    public ResponseEntity<?> getOrder(@PathVariable("orderid") String orderid)
    {
        EcomOrder order = ecomOrderRepository.findById(orderid).get();
        return ResponseEntity.ok(order);
    }

    // Get all orders for a given username
    @GetMapping("get/orders/{username}")
    public ResponseEntity<?> getOrders(@PathVariable("username") String username,
                                       @RequestHeader("Authorization") String token)
    {
        if(!authService.validateToken(token))
        {
            log.info("Invalid token: {}", token);
            return ResponseEntity.badRequest().body("Invalid token");
        }
        List<EcomOrder> orders = ecomOrderRepository.findByUsername(username);
        return ResponseEntity.ok(orders);
    }


    @PostMapping("create/order")
    public ResponseEntity<?> createOrderEcom(@RequestBody EcomOrder order,
                                         @RequestHeader("Authorization") String token,
                                         HttpServletRequest request,
                                         HttpServletResponse response) throws JsonProcessingException {

        // COOKIE VALIDATION LOGIC
        List<Cookie> cookieList = null;
        log.info("initiating cookie check");

        //Optional<String> healthStatusCookie = Optional.ofNullable(request.getHeader("health_status_cookie"));
        Cookie[] cookies = request.getCookies();
        if(cookies == null)
        {
            cookieList = new ArrayList<>();
        }
        else
        {
            // REFACTOR TO TAKE NULL VALUES INTO ACCOUNT
            cookieList = List.of(cookies);
        }
        log.info("cookie check complete");

        if( cookieList.stream().filter(cookie -> cookie.getName().equals("order-service-stage-1")).findAny().isEmpty()) // COOKIE_CHECK
        {
            log.info("Received request to create order: {}", order);
            if(authService.validateToken(token))
            {
                log.info("Token is valid: {}", token);
                log.info("Proceeding to create order: {}", order);
                order.setUsername(authService.getUsername(token));
                order.setOrderid(String.valueOf(new Random().nextInt(1000)));
                order.setStatus("PROCESSING");
                producer.publishOrderDatum(order.getOrderid(),
                        "CREATE",
                        "Order Created Successfully with Order ID: " + order.getOrderid(),
                        order.getStatus(),
                        order.getPayment_id());

                ecomOrderRepository.save(order);

                log.info("Order saved successfully: {}", order);

                redisTemplate.opsForValue().set(order.getOrderid(),"stage1 :   orderid:"+order.getOrderid());

                log.info("Checking inventory for the order");
                inventoryService.reserveProductStock(order,token);


                log.info("Creating a New Payment Request");
                PaymentRequest paymentRequest = new PaymentRequest();
                paymentRequest.setOrder_id(order.getOrderid());
                paymentRequest.setPayment_id(null);
                paymentRequest.setAmount(orderHelperService.calculateOrder(order, token));
                log.info("Payment Request created successfully: {}", paymentRequest);

                log.info("Sending request to Payment Service");
                // create payment
                String responseKey = paymentService.createPayment(paymentRequest, token);
                log.info("Received the ResponseKey which will be sent as a Cookie to the Front-end");

                log.info("Setting up the Cookie for the Front-end");
                Cookie cookieStage1 = new Cookie("order-service-stage-1", responseKey);
                cookieStage1.setMaxAge(300);
                log.info("Cookie set up successfully");

                response.addCookie(cookieStage1);
                log.info("Cookie added to the outgoing response");
                log.info("Order created successfully: {} and request forwarded to Payment Service", order);
                return ResponseEntity.ok("STAGE 1: We have started processing your Order with Order ID: " + order.getOrderid());
            }
            else
            {
                log.info("Token is invalid: {}", token);
                return ResponseEntity.badRequest().body("Invalid token");
            }


        }
        else
        {

            // FOLLOW UP LOGIC
            log.info("found a relevant cookie.. initiating follow up logic");

            Cookie followup_cookie =  cookieList.stream().
                    filter(cookie -> cookie.getName().equals("order-service-stage-1")).findAny().get();

            String followup_cookie_key = followup_cookie.getValue();
            String orderid = followup_cookie_key;
            String cacheResponse = (String)redisTemplate.opsForValue().get(followup_cookie_key);

            log.info("cacheResponse: " + cacheResponse);

            //String[] cacheResponseArray = cacheResponse.split(" ");

            if(cacheResponse.contains("stage1"))
            {
                log.info("Request still under process...");

                return ResponseEntity.ok("Request still under process...");
            }
            else if(cacheResponse.contains("Inventory reserved successfully"))
            {
                return ResponseEntity.ok("Inventory reserved successfully for Order ID: " + orderid);
            }
            else if(cacheResponse.contains("Checking Inventory"))
            {
                return ResponseEntity.ok("Checking Inventory for Order Created Successfully with Order ID: " + orderid);
            }
            else if(cacheResponse.contains("processing payment"))
            {
                return ResponseEntity.ok("Processing Payment for Order Created Successfully with Order ID: " + orderid);
            }
            else if(cacheResponse.contains("paymentid:orderid"))
            {
                return ResponseEntity.ok("payment successful. Order Created Successfully with Order ID: " + orderid);
            }
            else
            {
                return ResponseEntity.ok("Error Processing the Order");
            }


        }




    }


}
