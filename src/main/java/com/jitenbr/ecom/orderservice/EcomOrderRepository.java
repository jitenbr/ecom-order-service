package com.jitenbr.ecom.orderservice;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EcomOrderRepository extends MongoRepository<EcomOrder, String> {
    List<EcomOrder> findByUsername(String username);
}
